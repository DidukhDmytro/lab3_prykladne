# Project Guidelines

Apply these rules to every file generated or modified in this project:

- Use `com.example.carservice` as the root Java package; place project classes in that package or its domain-appropriate subpackages.
- Do not use `final` on classes, methods, parameters, or local variables. `final` is allowed and required on constants (`static final`) and dependency fields injected through Lombok `@RequiredArgsConstructor`.
- Constants use UPPER_SNAKE_CASE.
- Use strict Java naming conventions: UpperCamelCase for classes and lowerCamelCase for methods and fields. Keep fields private and expose only the accessors or operations the domain requires.
- Write all code comments and Javadoc strictly in English. Keep comments clear and concise, and use them to explain business rules where needed.
- Do not hardcode magic strings or numbers in output or exception checks. Define error messages, console output formats, and default constants as static variables.
- Keep code minimal and free of unnecessary, dead, or unused declarations.
