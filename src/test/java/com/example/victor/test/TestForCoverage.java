package com.example.victor.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class TestForCoverage {

    private Integer currentRecursionLevel = 0;

    private final Integer maxRecursionLevel = 5;

    private final Logger LOG = LoggerFactory.getLogger(TestForCoverage.class);

    public static final String PACKAGE_PREF = "com.example.victor.services";
    //public static final String PACKAGE_PREF = "com.example.victor.utils";
    //просто метод, из которого можно будет достать инстанс аннотации(?)
    //я не уверен, но, по моему, contains и содержащийся в нем equals должны работать быстрее, чем instanceof
    @Autowired
    public static void autowiredCarrier(){};

    @Test
    public void usefulTest(){
        //возьмет все классы из указанного пакета
        //главное, чтобы самого теста в этом пакете не оказалось
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new ResourcesScanner())
                .setUrls(ClasspathHelper.forPackage(PACKAGE_PREF))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(PACKAGE_PREF))));

        for(Class clazz : reflections.getSubTypesOf(Object.class)){

            LOG.debug("For class " + clazz.getSimpleName() + " started detour:");
            //конструируем инстанс "тестируемого" класса, попутно перебирая все конструкторы
            Object instance = generateObject(clazz, true);

            for (Method method : ReflectionUtils.getAllMethods(clazz,
                    ReflectionUtils.withModifier(Modifier.PUBLIC))) {

                LOG.debug("call method " + method.getName());

                ArrayList<Object> parameters = new ArrayList<>();

                //если весь метод отмечен как @Autowired, то все его параметры подменяем моками
                if(method.isAnnotationPresent(Autowired.class)){
                    for(Class parameterType : method.getParameterTypes()){
                        LOG.debug("creating a list of mocks for autowired method");
                        parameters.add(mock(parameterType));
                    }
                } else {
                    //иначе - надо смотреть на каждый параметр в отдельности
                    //главная трудность тут - если сказать parameterType.getAnnotation()
                    //то получится список аннотаций класса параметра, а не аннотации параметра метода
                    //поэтому приходится получать аннотации параметров всей кучей
                    Annotation[][] parametersAnnotations = method.getParameterAnnotations();
                    Integer i = 0;
                    for(Class parameterType : method.getParameterTypes()) {
                        try {
                            if (asList(parametersAnnotations[i++])
                                    .contains(getClass().getMethod("autowiredCarrier")
                                            .getAnnotation(Autowired.class))){
                                LOG.debug("creating single mock for autowired parameter");
                                parameters.add(mock(parameterType));
                            } else{
                                parameters.add(generateObject(parameterType, false));
                            }
                        } catch (NoSuchMethodException ignore){ }
                    }
                }

                try {
                    method.invoke(instance, parameters.toArray());
                } catch(Throwable ignore){
                    //метод может выбрасывать любые исключения - главное, чтобы тест шел дальше
                }
            }
        }
    }

    //в зависимости от значения флага функция либо переберет все конструкторы переданного класса
    //либо остановится после первого, который принес нужный инстанс
    //глубина рекурсии ограничена - чтобы не создавать миллион объектов или не попасться в вечный цикл
    //А еще можно напороться на вещь в духе A(A a)
    //на сколько хороша рекурсия с ограничением в виде поля класса - нинасколько
    //но чтобы сделать по другому надо перетряхнуть весь алгоритм и по хорошему избавиться от рекурсии
    private Object generateObject(Class clazz, Boolean callAllConstructors){
        LOG.debug("Current level of recursion = " + currentRecursionLevel);
        currentRecursionLevel++;
        if(clazz.isPrimitive()){
            currentRecursionLevel--;
            return generatePrimitive(clazz);
        }
        if(clazz.getSimpleName().equals("String")){
            currentRecursionLevel--;
            return "";
        }

        Object instance = null;
        Constructor[] constructors = clazz.getConstructors();

        for(Constructor constructor : clazz.getConstructors()){

            ArrayList<Object> constructorParameters = new ArrayList<>();

            Annotation[][] parametersAnnotations = constructor.getParameterAnnotations();
            Integer i = 0;
            for(Class parameterType : constructor.getParameterTypes()){
                try {
                    if (asList(parametersAnnotations[i++])
                            .contains(getClass().getMethod("autowiredCarrier")
                                    .getAnnotation(Autowired.class))) {
                        LOG.debug("creating mock for autowired parameter in constructor");
                        constructorParameters.add(mock(parameterType));
                    } else {
                        if(currentRecursionLevel < maxRecursionLevel) {
                            constructorParameters.add(generateObject(parameterType, false));
                        } else {
                            constructorParameters.add(mock(parameterType));
                        }
                    }
                } catch (NoSuchMethodException ignore) {}
            }
            try {
                instance = constructor.newInstance(constructorParameters.toArray());
            } catch(Exception ignore){

            }
            if(instance!=null){
                //если задача перебрать все конструкторы не ставилась, то после первого же сработавшего
                //можно выходить из функции
                if(!callAllConstructors){
                    //мокать @Autowired поля имеет смысл, если этот инстанс мы собираемся передать наружу
                    setAutowiredFields(instance);
                    currentRecursionLevel--;
                    return instance;
                }
            }
        }
        setAutowiredFields(instance);
        currentRecursionLevel--;
        //если целью стояло перебрать все конструкторы, то на выходе из цикла мы должны иметь искомый инстанс
        return instance;
    }

    private void setAutowiredFields(Object instance){
        for(Field field : instance.getClass().getDeclaredFields()){
            if(field.getAnnotation(Autowired.class) != null){
                LOG.debug("creating mock for autowired field!");
                field.setAccessible(true);
                try {
                    field.set(instance, mock(field.getType()));
                } catch (IllegalAccessException e){
                    LOG.debug(e.getMessage());
                }
            }
        }
    }

    private Object generatePrimitive(Class clazz){
        switch (clazz.getSimpleName()){
            case "double":
                return 1.0;
            case "short":
                return 1;
            case "boolean":
                return  true;
            case "float":
                return 1.0;
            case "byte":
                return 1;
            case "long":
                return 1l;
            case "char":
                return '1';
            case "int":
                return 1;
            default:
                return null;
        }
    }
}
