package __PACKAGE_NAME__.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Slf4j
public class CodeGenerator {

    private static final String AUTHOR = "generator";
    private static final boolean FILE_OVERRIDE = false;
    private static final String[] TABLE_PREFIXES = {"t_", "tbl_"};

    private static final String PROJECT_ROOT = findProjectRoot();
    private static final String PROJECT_NAME = loadProjectName();

    private static final String INFRA_JAVA = PROJECT_ROOT + "/" + PROJECT_NAME + "-infrastructure/src/main/java";
    private static final String INFRA_RES = PROJECT_ROOT + "/" + PROJECT_NAME + "-infrastructure/src/main/resources";
    private static final String APP_JAVA = PROJECT_ROOT + "/" + PROJECT_NAME + "-app/src/main/java";

    public static void main(String[] args) {
        Map<String, Object> dbConfig = loadDbConfig();
        String url = (String) dbConfig.get("url");
        String username = (String) dbConfig.get("username");

        System.out.println("========================================");
        System.out.println("  DB URL:      " + url);
        System.out.println("  DB Username: " + username);
        System.out.println("  Project:     " + PROJECT_NAME);
        System.out.println("========================================");

        String password = readPassword();
        String table = readTableName();

        if (args.length > 0 && "--list".equals(args[0])) {
            listTables(url, username, password);
            return;
        }

        generateInfrastructure(url, username, password, table);
        generateApp(url, username, password, table);
    }

    private static void generateInfrastructure(String url, String username, String password, String table) {
        String tablePkg = toPackageName(table);
        String basePackage = loadBasePackage();

        Map<OutputFile, String> pathInfo = new HashMap<>();
        pathInfo.put(OutputFile.xml, INFRA_RES + "/" + basePackage.replace('.', '/') + "/infrastructure/" + tablePkg + "/mapper");

        mkdirs(INFRA_JAVA, INFRA_RES);

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author(AUTHOR)
                        .outputDir(INFRA_JAVA)
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
                    builder.addTablePrefix(TABLE_PREFIXES);
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
                    if (FILE_OVERRIDE) {
                        builder.entityBuilder().enableFileOverride();
                        builder.mapperBuilder().enableFileOverride();
                    }
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        log.info("[{}-infrastructure] Generated", PROJECT_NAME);
        log.info("  Entity -> {}/{}/infrastructure/{}/entity/", INFRA_JAVA, basePackage.replace('.', '/'), tablePkg);
        log.info("  Mapper -> {}/{}/infrastructure/{}/mapper/", INFRA_JAVA, basePackage.replace('.', '/'), tablePkg);
        log.info("  XML    -> {}/{}/infrastructure/{}/mapper/", INFRA_RES, basePackage.replace('.', '/'), tablePkg);
    }

    private static void generateApp(String url, String username, String password, String table) {
        String tablePkg = toPackageName(table);
        String basePackage = loadBasePackage();

        mkdirs(APP_JAVA);

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author(AUTHOR)
                        .outputDir(APP_JAVA)
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
                    builder.addTablePrefix(TABLE_PREFIXES);
                    builder.serviceBuilder()
                            .formatServiceFileName("I%sService");
                    builder.mapperBuilder().disable();
                    builder.controllerBuilder().disable();
                    if (FILE_OVERRIDE) {
                        builder.serviceBuilder().enableFileOverride();
                    }
                })
                .templateConfig(builder -> builder
                        .disable(TemplateType.ENTITY)
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        deleteQuietly(new File(APP_JAVA + "/" + basePackage.replace('.', '/') + "/infrastructure"));

        log.info("[{}-app] Generated", PROJECT_NAME);
        log.info("  IService    -> {}/{}/app/{}/", APP_JAVA, basePackage.replace('.', '/'), tablePkg);
        log.info("  ServiceImpl -> {}/{}/app/{}/impl/", APP_JAVA, basePackage.replace('.', '/'), tablePkg);
    }

    public static void listTables(String url, String username, String password) {
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

    private static String findProjectRoot() {
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

    private static String loadProjectName() {
        Map<String, Object> config = loadYamlConfig();
        Object name = getNestedValue(config, "spring", "application", "name");
        if (name != null) {
            return name.toString();
        }
        File[] dirs = new File(System.getProperty("user.dir")).getParentFile().listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir.getName().endsWith("-infrastructure")) {
                    return dir.getName().replace("-infrastructure", "");
                }
            }
        }
        throw new IllegalStateException("Cannot determine project name from application.yml or directory structure");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadDbConfig() {
        Map<String, Object> config = loadYamlConfig();
        Map<String, Object> result = new HashMap<>();

        Object url = getNestedValue(config, "spring", "datasource", "url");
        Object username = getNestedValue(config, "spring", "datasource", "username");

        if (url == null || username == null) {
            throw new IllegalStateException("Cannot find spring.datasource.url/username in application.yml");
        }

        result.put("url", url.toString());
        result.put("username", username.toString());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static String loadBasePackage() {
        Map<String, Object> config = loadYamlConfig();
        Object pkg = getNestedValue(config, "mybatis-plus", "type-aliases-package");
        if (pkg != null) {
            String fullPkg = pkg.toString();
            int idx = fullPkg.indexOf(".infrastructure.");
            if (idx > 0) {
                return fullPkg.substring(0, idx);
            }
        }
        throw new IllegalStateException("Cannot determine base package from mybatis-plus.type-aliases-package in application.yml");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYamlConfig() {
        String[] searchPaths = {
                PROJECT_ROOT + "/" + PROJECT_NAME + "-start/src/main/resources/application.yml",
                PROJECT_ROOT + "/" + PROJECT_NAME + "-start/src/main/resources/application.yaml",
        };

        for (String path : searchPaths) {
            File file = new File(path);
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> result = yaml.load(is);
                    return result != null ? result : new HashMap<>();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse " + path, e);
                }
            }
        }
        throw new IllegalStateException("application.yml not found in " + PROJECT_NAME + "-start module");
    }

    @SuppressWarnings("unchecked")
    private static Object getNestedValue(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null;
            }
        }
        return current;
    }

    private static String readPassword() {
        if (System.console() != null) {
            return new String(System.console().readPassword("Enter database password: "));
        }
        System.out.print("Enter database password (visible in IDE): ");
        return new Scanner(System.in).nextLine();
    }

    private static String readTableName() {
        System.out.print("Enter table name: ");
        return new Scanner(System.in).nextLine().trim();
    }

    private static String toPackageName(String tableName) {
        return tableName.toLowerCase().replace("_", "");
    }

    private static void mkdirs(String... paths) {
        for (String path : paths) {
            new File(path).mkdirs();
        }
    }

    private static void deleteQuietly(File file) {
        if (!file.exists()) {
            return;
        }
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
