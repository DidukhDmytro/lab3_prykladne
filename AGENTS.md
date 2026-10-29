# Project Guidelines

Apply these rules to every file generated or modified in this project:

- Use `com.example.carservice` as the root Java package; place project classes in that package or its domain-appropriate subpackages.
- Do not use the `final` keyword anywhere, including on fields, variables, parameters, and classes.
- Use strict Java naming conventions: UpperCamelCase for classes and lowerCamelCase for methods and fields. Keep fields private and expose only the accessors or operations the domain requires.
- Write all code comments and Javadoc strictly in English. Keep comments clear and concise, and use them to explain business rules where needed.
- Do not hardcode magic strings or numbers in output or exception checks. Define error messages, console output formats, and default constants as static variables.
- Keep code minimal and free of unnecessary, dead, or unused declarations.
