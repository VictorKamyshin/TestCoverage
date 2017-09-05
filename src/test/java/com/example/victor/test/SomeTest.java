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

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SomeTest {

    private final Logger LOG = LoggerFactory.getLogger(SomeTest.class);

    public static final String PACKAGE_PREF = "com.example.victor.services";

    //просто метод, из которого можно будет достать инстанс аннотации(?)
    @Autowired
    public static void autowiredCarrier(){};

    @Test
    public void usefulTest(){
        //возьмет все классы из пакета с указанным префиксом
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
                            if (Arrays.asList(parametersAnnotations[i++])
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
    private Object generateObject(Class clazz, Boolean callAllConstructors){
        if(clazz.isPrimitive()){
            return generatePrimitive(clazz);
        }
        if(clazz.getSimpleName().equals("String")){
            return "";
        }

        Object instance = null;
        for(Constructor constructor : clazz.getConstructors()){

            ArrayList<Object> constructorParameters = new ArrayList<>();

            Annotation[][] parametersAnnotations = constructor.getParameterAnnotations();
            Integer i = 0;
            for(Class parameterType : constructor.getParameterTypes()){
                try {
                    if (Arrays.asList(parametersAnnotations[i++])
                            .contains(getClass().getMethod("autowiredCarrier")
                                    .getAnnotation(Autowired.class))) {
                        LOG.debug("creating mock for autowired parameter in constructor");
                        constructorParameters.add(mock(parameterType));
                    } else {
                        constructorParameters.add(generateObject(parameterType,false));
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
                    //мокать @Autowired поля имеет смысл, если этот инстанс мы собираемся вернуть
                    setAutowiredFields(instance);
                    return instance;
                }
            }
        }
        setAutowiredFields(instance);
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
