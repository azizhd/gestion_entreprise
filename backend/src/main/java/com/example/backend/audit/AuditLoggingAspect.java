package com.example.backend.audit;

import com.example.backend.service.AuditLogService;
import com.example.backend.service.AuditLogService.AuditLogRecord;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Aspect
@Component
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;

    public AuditLoggingAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Pointcut("@annotation(com.example.backend.audit.AuditAction)")
    public void annotatedMethods() {}

    @Around(value = "annotatedMethods() && @annotation(auditAction)")
    public Object logAction(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        // Avoid auditing the audit logger itself to prevent infinite recursion
        if (joinPoint.getTarget() instanceof AuditLogService) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String action = auditAction.action();
        String entityType = !auditAction.entityType().isBlank()
                ? auditAction.entityType()
                : joinPoint.getTarget().getClass().getSimpleName();
        Long entityId = resolveEntityId(signature, joinPoint.getArgs());
        String principalEmail = resolvePrincipalEmail(joinPoint.getArgs());

        boolean success = false;
        String errorMessage = null;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            String contenu = buildContenu(action, entityType, entityId, success);
            auditLogService.record(new AuditLogRecord(
                    action,
                    auditAction.type(),
                    contenu,
                    entityType,
                    entityId,
                    success,
                    errorMessage,
                    principalEmail
            ));
        }
    }

    private String buildContenu(String action, String entityType, Long entityId, boolean success) {
        StringBuilder builder = new StringBuilder();
        builder.append(action).append(" on ").append(entityType);
        if (entityId != null) {
            builder.append(" [id=").append(entityId).append(']');
        }
        builder.append(success ? " succeeded" : " failed");
        return builder.toString();
    }

    private Long resolveEntityId(MethodSignature signature, Object[] args) {
        String[] parameterNames = signature.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            if (arg instanceof Long) {
                return (Long) arg;
            }
            if (arg instanceof Integer) {
                return ((Integer) arg).longValue();
            }
            if (parameterNames != null && parameterNames.length > i) {
                String name = parameterNames[i];
                if (name != null && name.toLowerCase().endsWith("id") && arg instanceof Number number) {
                    return number.longValue();
                }
            }
        }
        return null;
    }

    private String resolvePrincipalEmail(Object[] args) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        Optional<String> emailArg = Arrays.stream(args)
            .filter(arg -> arg instanceof String)
            .map(Object::toString)
            .filter(val -> val.contains("@"))
            .findFirst();
        return emailArg.orElse(null);
    }
}
