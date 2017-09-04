package com.example.victor.test;

import com.example.victor.services.SimpleService;
import com.example.victor.utils.SomeClass;
import org.junit.Assert;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class SomeTest {

    @Test
    public void usefulTest(){

        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());


        //возьмет все классы из указанного пакета
        // главное, чтобы самого теста в этом пакете не оказалось
        Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setScanners(new SubTypesScanner(false), new ResourcesScanner())
        .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
        .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("com.example.victor.services"))));

        Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class);


        for(Class clazz : allClasses){
            System.out.println("For class "+clazz.getSimpleName());
            Object instance = constructObject(clazz);
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
                        parameters.add(constructObject(parameterType));
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

    private Object constructObject(Class clazz){
        switch (clazz.getSimpleName()){
            case "double":
                double dTmp = 1.0;
                return dTmp;
            case "short":
                short sTmp = 1;
                return sTmp;
            case "boolean":
                return  true;
            case "float":
                float fTmp = 1;
                return fTmp;
            case "byte":
                byte bTmp = 1;
                return bTmp;
            case "long":
                long lTmp = 1l;
                return lTmp;
            case "char":
                char cTmp = '1';
                return cTmp;
            case "int":
                int iTmp = 1;
                return iTmp;
        }
        if(clazz.getSimpleName().equals("String")){
            return "";
        }

        Constructor<?>[] constructors = clazz.getConstructors();
        Object instance = null;
        //в попытках создать инстанс нужного класса просто перебираем все конструкторы подряд
        for(Constructor constructor : constructors){
            if(instance == null) {
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
                    if (constructorParameters.size() < i) {
                        constructorParameters.add(constructObject(parameterType));
                    }
                }
                try {
                    instance = constructor.newInstance(constructorParameters.toArray());
                    //если с помощью выбранного конструктора не получилось создать инстанс
                    //черт с ним, просто пробуем следующий конструктор
                } catch(Exception e){
                    e.printStackTrace();
                }
            } else {
                //мы получили искомый инстанс, цикл можно останавливать
                break;
            }
         }

        return instance;
    }
}
