package com.demkom58.springram.controller;

import com.demkom58.springram.controller.annotation.BotController;
import com.demkom58.springram.controller.annotation.CommandMapping;
import com.demkom58.springram.controller.config.PathMatchingConfigurer;
import com.demkom58.springram.controller.config.TelegramMvcConfigurerComposite;
import com.demkom58.springram.controller.method.argument.HandlerMethodArgumentResolver;
import com.demkom58.springram.controller.method.argument.HandlerMethodArgumentResolverComposite;
import com.demkom58.springram.controller.method.argument.impl.PathVariablesHandlerMethodArgumentResolver;
import com.demkom58.springram.controller.method.result.HandlerMethodReturnValueHandler;
import com.demkom58.springram.controller.method.result.HandlerMethodReturnValueHandlerComposite;
import com.demkom58.springram.controller.method.result.impl.SendMessageHandlerMethodReturnValueHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.*;

public class UpdateBeanPostProcessor implements BeanPostProcessor, Ordered {
    private final Map<String, Class<?>> botControllerMap = new HashMap<>();
    private final CommandContainer commandContainer;

    public UpdateBeanPostProcessor(TelegramCommandDispatcher commandDispatcher,
                                   CommandContainer commandContainer,
                                   TelegramMvcConfigurerComposite configurerComposite) {
        final HandlerMethodArgumentResolverComposite argumentResolvers
                = new HandlerMethodArgumentResolverComposite();

        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        configurerComposite.configureArgumentResolvers(resolvers);
        resolvers.addAll(createArgumentResolvers());
        argumentResolvers.addAll(resolvers);

        final HandlerMethodReturnValueHandlerComposite returnValueHandlers
                = new HandlerMethodReturnValueHandlerComposite();

        List<HandlerMethodReturnValueHandler> returnHandlers = new ArrayList<>();
        configurerComposite.configureReturnValueHandlers(returnHandlers);
        returnHandlers.addAll(createReturnValueHandlers());
        returnValueHandlers.addAll(returnHandlers);

        commandDispatcher.setReturnValueHandlers(returnValueHandlers);
        commandDispatcher.setArgumentResolvers(argumentResolvers);

        this.commandContainer = commandContainer;
        PathMatchingConfigurer pathMatchingConfigurer = new PathMatchingConfigurer();
        configurerComposite.configurePathMatcher(pathMatchingConfigurer);
        commandContainer.setPathMatchingConfigurer(pathMatchingConfigurer);
    }

    private List<HandlerMethodArgumentResolver> createArgumentResolvers() {
        return List.of(
                new PathVariablesHandlerMethodArgumentResolver()
        );
    }

    private List<HandlerMethodReturnValueHandler> createReturnValueHandlers() {
        return List.of(
                new SendMessageHandlerMethodReturnValueHandler()
        );
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        final Class<?> beanClass = bean.getClass();

        if (beanClass.isAnnotationPresent(BotController.class)) {
            botControllerMap.put(beanName, beanClass);
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        final Class<?> original = botControllerMap.get(beanName);
        if (original == null) {
            return bean;
        }

        Arrays.stream(original.getMethods())
                .filter(method -> method.isAnnotationPresent(CommandMapping.class))
                .forEach((Method method) -> commandContainer.addMethod(bean, method));

        return bean;
    }

    @Override
    public int getOrder() {
        return 100;
    }

}