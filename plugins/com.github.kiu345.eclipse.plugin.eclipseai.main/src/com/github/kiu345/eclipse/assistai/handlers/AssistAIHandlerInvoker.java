package com.github.kiu345.eclipse.assistai.handlers;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;

import com.github.kiu345.eclipse.assistai.Activator;
import com.github.kiu345.eclipse.assistai.prompt.Prompts;

public class AssistAIHandlerInvoker {
    public static void Invoke(String command) {
        Invoke(Prompts.valueOf(command.replace(' ', '_').toUpperCase().substring(1)));
    }

    public static void Invoke(Prompts prompt) {
        try {
            switch (prompt) {
                case SYSTEM:
                    break;
                case DISCUSS:
                    ((AssistAIDiscussCodeHandler) ContextInjectionFactory.make(
                            Activator.getBundleContext().getBundle().loadClass(AssistAIDiscussCodeHandler.class.getName()),
                            EclipseContextFactory.getServiceContext(Activator.getBundleContext())
                    )).runPrompt();
                    ;
                    break;
                case DOCUMENT:
                    ((AssistAIJavaDocHandler) ContextInjectionFactory.make(
                            Activator.getBundleContext().getBundle().loadClass(AssistAIJavaDocHandler.class.getName()),
                            EclipseContextFactory.getServiceContext(Activator.getBundleContext())
                    )).runPrompt();
                    ;
                    break;
                case FIX_ERRORS:
                    ((AssistAIFixErrorsHandler) ContextInjectionFactory.make(
                            Activator.getBundleContext().getBundle().loadClass(AssistAIFixErrorsHandler.class.getName()),
                            EclipseContextFactory.getServiceContext(Activator.getBundleContext())
                    )).runPrompt();
                    ;
                    break;
                case GIT_COMMENT:
                    ((AssistAIGenerateGitCommentHandler) ContextInjectionFactory.make(
                            Activator.getBundleContext().getBundle().loadClass(AssistAIGenerateGitCommentHandler.class.getName()),
                            EclipseContextFactory.getServiceContext(Activator.getBundleContext())
                    )).runPrompt();
                    ;
                    break;
                case REFACTOR:
                    ((AssistAICodeRefactorHandler) ContextInjectionFactory.make(
                            Activator.getBundleContext().getBundle().loadClass(AssistAICodeRefactorHandler.class.getName()),
                            EclipseContextFactory.getServiceContext(Activator.getBundleContext())
                    )).runPrompt();
                    ;
                    break;
                case TEST_CASE:
                    ((AssistAIUnitTestHandler) ContextInjectionFactory.make(
                            Activator.getBundleContext().getBundle().loadClass(AssistAIUnitTestHandler.class.getName()),
                            EclipseContextFactory.getServiceContext(Activator.getBundleContext())
                    )).runPrompt();
                    ;
                    break;
                case UPGRADE_SOURCE:
                    ((AssistAIJavaUpgradeHandler) ContextInjectionFactory.make(
                            Activator.getBundleContext().getBundle().loadClass(AssistAIJavaUpgradeHandler.class.getName()),
                            EclipseContextFactory.getServiceContext(Activator.getBundleContext())
                    )).runPrompt();
                    ;
                    break;
                case DISCUSS_SELECTED:
                    break;
            }
        }
        catch (ClassNotFoundException e) {
        }

    }
}
