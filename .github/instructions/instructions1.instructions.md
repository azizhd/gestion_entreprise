---
description: These instructions must be loaded for every prompt to ensure architectural consistency and PrimeNG integration.
# applyTo: 'frontend and backend tasks'
---

1. Stack & Scope
- Backend: Spring Boot 3.5 (Java 17), JWT, JPA + MySQL, MapStruct, Bean Validation, Mail, Thymeleaf, PDFBox, OpenAPI.
- Frontend: Angular 17 (Standalone Components).
- UI Library: PrimeNG (Mandatory).
- Do not introduce alternative UI frameworks (Material, Bootstrap) unless explicitly requested.

2. Architecture Rules (Strict)
- Layered architecture: controller → service → repository.
- Controllers: Use DTOs only. No business logic. Structured error responses.
- Services: Business logic, @Transactional, ownership/role validation. Constructor injection only.
- Repositories: Thin data access. Use pagination. Prevent N+1.

3. Security Rules (Mandatory)
- JWT stateless sessions. BCrypt for passwords.
- Public endpoints: /api/auth/**, /swagger-ui/**, /v3/api-docs/**, /health.
- Never log secrets/tokens. Derive identity from SecurityContext.

4. DTO & Mapping
- Centralize mapping via MapStruct. Prefer immutable records for DTOs.

5. Validation & Error Handling
- Use javax.validation on DTOs. Consistent error formats. No stack traces in production.

6. Logging
- Use @Slf4j. Log meaningful events only. No sensitive data.

7. Coding Conventions
- Constructor injection only. No wildcard imports. Small, single-responsibility methods.

8. Pagination & Performance
- All list endpoints must support pagination. Avoid unbounded queries.

9. PrimeNG Setup & Standards (MANDATORY)
Before proceeding with any UI task, verify the following. If missing, suggest the setup steps:
- Installation: Ensure 'primeng', 'primeicons', and 'primeflex' are in package.json.
- Global Config: Ensure 'provideAnimationsAsync()' and PrimeNG configuration are in 'app.config.ts'.
- Styles: Verify 'primeng.min.css', 'primeicons.css', and a theme (e.g., 'lara-light-blue') are in 'angular.json'.
- Component Usage:
  - Use PrimeNG components for all UI elements (p-button, p-table, p-dialog, etc.).
  - Import specific PrimeNG Modules (e.g., ButtonModule) directly into the 'imports' array of standalone components.
  - Prefer PrimeFlex for layout/spacing over custom CSS.

10. Frontend (Angular 17)
- Use standalone components. Keep logic in services.
- Use OnPush change detection for pure UI components.
- Centralize JWT and Error handling in Interceptors.
- Use strong typing (Interfaces/Types) for all API responses.

11. Testing Rules
- Add/adjust tests for behavior changes. Use @WebMvcTest and @DataJpaTest. Mock external services.

12. Documentation
- Update README for new dependencies, env vars, or setup steps.

13. Output Style for Copilot
- Concise, actionable code. Minimal changes. Preserve existing patterns. Default to ASCII.

14. Non-Negotiable Constraints
- Always use PrimeNG for UI tasks.
- Never expose entities directly.
- Never bypass the service layer.
- Never trust frontend-provided ownership data.
- Always default to paginated list endpoints.

End of instructions.