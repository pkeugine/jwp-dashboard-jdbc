package nextstep.mvc.controller.tobe;

import nextstep.web.annotation.Autowired;
import nextstep.web.annotation.Controller;
import nextstep.web.annotation.Repository;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ControllerScanner {

    private static final Logger log = LoggerFactory.getLogger(ControllerScanner.class);

    private final Reflections reflections;

    public ControllerScanner(Object... basePackage) {
        reflections = new Reflections(basePackage);
    }

    public Map<Class<?>, Object> getControllers() {
        Set<Class<?>> preInitiatedControllers = reflections.getTypesAnnotatedWith(Controller.class);
        return instantiateControllers(preInitiatedControllers);
    }

    private Map<Class<?>, Object> instantiateControllers(Set<Class<?>> preInitiatedControllers) {
        final Map<Class<?>, Object> controllers = new HashMap<>();
        try {
            for (Class<?> clazz : preInitiatedControllers) {
                Constructor<?> autowiredConstructor = scanAutowiredConstructor(clazz);
                if (autowiredConstructor == null) {
                    Object classInstance = clazz.getDeclaredConstructor().newInstance();
                    controllers.put(clazz, classInstance);
                    continue;
                }
                Object daoInstance = createDaoInstance(autowiredConstructor);
                Object classInstance = autowiredConstructor.newInstance(daoInstance);
                controllers.put(clazz, classInstance);
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            log.error(exception.getMessage());
        }

        return controllers;
    }

    private Constructor<?> scanAutowiredConstructor(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .findFirst()
                .orElse(null);
    }

    private Object createDaoInstance(Constructor<?> autowiredConstructors) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Set<Class<?>> repositories = reflections.getTypesAnnotatedWith(Repository.class);
        Class<?> daoClass = Arrays.stream(autowiredConstructors.getParameters())
                .map(Parameter::getType)
                .filter(repositories::contains)
                .findFirst()
                .orElseThrow();
        Object daoInstance = daoClass.getDeclaredConstructor().newInstance();
        return daoInstance;
    }
}
