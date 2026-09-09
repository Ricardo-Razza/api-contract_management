# Stage 1: Build da aplicação com Maven e Java 21
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia o pom.xml e baixa dependências (cache otimizado)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código fonte e compila o pacote .jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Imagem final super leve com JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuário seguro sem privilégios root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia o JAR gerado
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta configurada no seu application.yml
EXPOSE 8081

# Executa com timezone correto do Brasil
ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
