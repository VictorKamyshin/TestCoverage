package com.example.victor.test;

import org.junit.Test;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class SomeTest {

    public static final String PACKAGE_PREF = "com.example.victor.services";

    @Test
    public void usefulTest(){

        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        //возьмет все классы из пакета с указанным префиксом
        // главное, чтобы самого теста в этом пакете не оказалось
        Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setScanners(new SubTypesScanner(false), new ResourcesScanner())
        .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
        .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(PACKAGE_PREF))));

        Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class);

        for(Class clazz : allClasses){
            System.out.println("For class "+clazz.getSimpleName());
            Object instance = generateObject(clazz, true);
//            Object instance = callAllConstructors(clazz);
            Set<Method> methods = ReflectionUtils.getAllMethods(clazz,
                    ReflectionUtils.withModifier(Modifier.PUBLIC));

            for (Method method : methods) {
                System.out.println(">>>" + method.getName());
                ArrayList<Object> parameters = new ArrayList<>();

                Annotation[][] paramAnnotations = method.getParameterAnnotations();
                Class[] paramTypes = method.getParameterTypes();
                int i = 0;
                for(Annotation[] annotations : paramAnnotations) {
                    Class parameterType = paramTypes[i++];
                    //идем по списку параметров метода
                    //если поле отмечено аннотацией @Autowired
                    //то подменяем его моком, иначе пытаемся сконструировать подходящий объект
                    for (Annotation annotation : annotations) {
                        if (annotation instanceof Autowired) {
                            System.out.println("Mock time!");
                            parameters.add(mock(parameterType));
                        }
                    }
                    //если в списке аргументов меньше, чем счетчик циклов, то это значит
                    //что на текущей итерации в него ничего не добавляли
                    //следовательно, параметр метода не был отмечен аннотацией и подменен моком
                    // и его надо просто создать
                    if (parameters.size() < i) {
                        parameters.add(generateObject(parameterType,false));
                        //parameters.add(constructObject(parameterType));
                    }
                }
                try {
                    method.invoke(instance, parameters.toArray());
                } catch(Exception e){
                    //метод может выбрасывать любые исключения - главное, чтобы тест шел дальше
                    e.printStackTrace();
                }
            }

        }
    }

    //в зависимости от значения флага функция либо переберет все конструкторы переданного класса
    //либо остановится после первого, который принес инстанс нужного класса
    private Object generateObject(Class clazz, Boolean callAllConstructors){
        if(clazz.isPrimitive()){
            return generatePrimitive(clazz);
        }
        if(clazz.getSimpleName().equals("String")){
            return "";
        }

        Constructor<?>[] constructors = clazz.getConstructors();
        Object instance = null;
        for(Constructor constructor : constructors){

            ArrayList<Object> constructorParameters = new ArrayList<>();
            Annotation[][] paramAnnotations = constructor.getParameterAnnotations();
            Class[] paramTypes = constructor.getParameterTypes();

            int i = 0;
            for(Annotation[] annotations : paramAnnotations) {
                Class parameterType = paramTypes[i++];

                for (Annotation annotation : annotations) {
                    if (annotation instanceof Autowired) {
                        constructorParameters.add(mock(parameterType));
                    }
                }
                //если список параметров меньше номера текущей итерации, значит
                //параметра, помеченного как @Autowired не нашлось и надо
                //его создать самим, рекурсивно, с помощью этой же функции
                if (constructorParameters.size() < i) {
                    constructorParameters.add(generateObject(parameterType,false));
                }
            }
            try {
                instance = constructor.newInstance(constructorParameters.toArray());
            } catch(Exception e){
                e.printStackTrace();
            }
            //если задача перебрать все конструкторы не ставилась, то после первого же сработавшего
            //можно выходить из функции
            if(!callAllConstructors){
                if(instance!=null){
                    return instance;
                }
            }
        }
        return instance;
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
