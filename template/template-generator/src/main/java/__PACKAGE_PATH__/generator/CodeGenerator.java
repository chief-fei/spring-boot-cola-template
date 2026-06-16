package __PACKAGE_NAME__.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenerator implements CommandLineRunner {

    private final GeneratorProperties props;

    @Autowired
    private Environment env;

    @Override
    public void run(String... args) throws Exception {
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String projectName = env.getProperty("spring.application.name", "app");

        if (url == null || username == null) {
            throw new IllegalStateException(
                    "Cannot find spring.datasource.url/username. " +
                    "Run with: --spring.config.import=file:../{project}-start/src/main/resources/application.yml");
        }

        String projectRoot = findProjectRoot();
        String infraJava = projectRoot + "/" + projectName + "-infrastructure/src/main/java";
        String infraRes = projectRoot + "/" + projectName + "-infrastructure/src/main/resources";
        String appJava = projectRoot + "/" + projectName + "-app/src/main/java";

        System.out.println("========================================");
        System.out.println("  DB URL:      " + url);
        System.out.println("  DB Username: " + username);
        System.out.println("  Project:     " + projectName);
        System.out.println("========================================");

        String password = readPassword();
        String table = readTableName();

        if (args.length > 0 && "--list".equals(args[0])) {
            listTables(url, username, password);
            return;
        }

        generateInfrastructure(url, username, password, table, projectName, infraJava, infraRes);
        generateApp(url, username, password, table, projectName, appJava);
    }

    private void generateInfrastructure(String url, String username, String password,
                                        String table, String projectName,
                                        String infraJava, String infraRes) {
        String tablePkg = toPackageName(table);
        String basePackage = props.getBasePackage();

        Map<OutputFile, String> pathInfo = new HashMap<>();
        pathInfo.put(OutputFile.xml, infraRes + "/" + basePackage.replace('.', '/')
                + "/infrastructure/" + tablePkg + "/mapper");

        mkdirs(infraJava, infraRes);

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author(props.getAuthor())
                        .outputDir(infraJava)
                        .disableOpenDir()
                        .commentDate("yyyy-MM-dd")
                )
                .packageConfig(builder -> builder
                        .parent(basePackage + ".infrastructure." + tablePkg)
                        .entity("entity")
                        .mapper("mapper")
                        .pathInfo(pathInfo)
                )
                .strategyConfig(builder -> {
                    builder.addInclude(table);
                    builder.addTablePrefix(props.getTablePrefixes());
                    builder.entityBuilder()
                            .enableLombok()
                            .enableChainModel()
                            .enableTableFieldAnnotation();
                    builder.mapperBuilder()
                            .enableMapperAnnotation()
                            .enableBaseResultMap()
                            .enableBaseColumnList();
                    builder.serviceBuilder().disable();
                    builder.controllerBuilder().disable();
                    if (props.isFileOverride()) {
                        builder.entityBuilder().enableFileOverride();
                        builder.mapperBuilder().enableFileOverride();
                    }
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        log.info("[{}-infrastructure] Generated", projectName);
        log.info("  Entity -> {}/{}/infrastructure/{}/entity/", infraJava, basePackage.replace('.', '/'), tablePkg);
        log.info("  Mapper -> {}/{}/infrastructure/{}/mapper/", infraJava, basePackage.replace('.', '/'), tablePkg);
        log.info("  XML    -> {}/{}/infrastructure/{}/mapper/", infraRes, basePackage.replace('.', '/'), tablePkg);
    }

    private void generateApp(String url, String username, String password,
                             String table, String projectName, String appJava) {
        String tablePkg = toPackageName(table);
        String basePackage = props.getBasePackage();

        mkdirs(appJava);

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author(props.getAuthor())
                        .outputDir(appJava)
                        .disableOpenDir()
                        .commentDate("yyyy-MM-dd")
                )
                .packageConfig(builder -> builder
                        .parent(basePackage)
                        .entity("infrastructure." + tablePkg + ".entity")
                        .mapper("infrastructure." + tablePkg + ".mapper")
                        .service("app." + tablePkg)
                        .serviceImpl("app." + tablePkg + ".impl")
                )
                .strategyConfig(builder -> {
                    builder.addInclude(table);
                    builder.addTablePrefix(props.getTablePrefixes());
                    builder.serviceBuilder()
                            .formatServiceFileName("I%sService");
                    builder.mapperBuilder().disable();
                    builder.controllerBuilder().disable();
                    if (props.isFileOverride()) {
                        builder.serviceBuilder().enableFileOverride();
                    }
                })
                .templateConfig(builder -> builder
                        .disable(TemplateType.ENTITY)
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        deleteQuietly(new File(appJava + "/" + basePackage.replace('.', '/') + "/infrastructure"));

        log.info("[{}-app] Generated", projectName);
        log.info("  IService    -> {}/{}/app/{}/", appJava, basePackage.replace('.', '/'), tablePkg);
        log.info("  ServiceImpl -> {}/{}/app/{}/impl/", appJava, basePackage.replace('.', '/'), tablePkg);
    }

    private void listTables(String url, String username, String password) {
        System.out.println("====== Database Tables ======");
        try (Connection conn = DriverManager.getConnection(url, username, password);
             ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                System.out.println("  " + rs.getString("TABLE_NAME"));
            }
        } catch (Exception e) {
            System.err.println("Query failed: " + e.getMessage());
        }
        System.out.println("=============================");
    }

    private String findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            File[] dirs = current.toFile().listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir.getName().endsWith("-infrastructure")) {
                        return current.toString();
                    }
                }
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "Cannot find project root. Run from generator module or project root (need *-infrastructure subdirectory)");
    }

    private String readPassword() {
        if (System.console() != null) {
            return new String(System.console().readPassword("Enter database password: "));
        }
        System.out.print("Enter database password (visible in IDE): ");
        return new Scanner(System.in).nextLine();
    }

    private String readTableName() {
        System.out.print("Enter table name: ");
        return new Scanner(System.in).nextLine().trim();
    }

    private String toPackageName(String tableName) {
        return tableName.toLowerCase().replace("_", "");
    }

    private void mkdirs(String... paths) {
        for (String path : paths) {
            new File(path).mkdirs();
        }
    }

    private void deleteQuietly(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteQuietly(child);
                }
            }
        }
        file.delete();
    }
}
