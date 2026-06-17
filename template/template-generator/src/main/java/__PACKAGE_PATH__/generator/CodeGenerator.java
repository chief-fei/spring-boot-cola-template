package __PACKAGE_NAME__.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenerator implements CommandLineRunner {

    private final DataSource dataSource;
    private final GeneratorProperties props;

    @Override
    public void run(String... args) throws Exception {
        String projectName = resolveProjectName();

        if (args.length > 0 && "--list".equals(args[0])) {
            listTables();
            return;
        }

        String table = resolveTableName(args);
        if (table == null || table.isEmpty()) {
            throw new IllegalStateException(
                    "请指定表名: --table=<表名> 或在 generator.table / application.yml 中配置");
        }

        String projectRoot = System.getProperty("user.dir");
        String infraJava = projectRoot + "/src/main/java";
        String infraRes = projectRoot + "/src/main/resources";
        String appJava = projectRoot.replace("-generator", "-app") + "/src/main/java";

        System.out.println("========================================");
        System.out.println("  Project: " + projectName);
        System.out.println("  Table:   " + table);
        System.out.println("========================================");

        generateInfrastructure(table, infraJava, infraRes);
        generateApp(table, appJava);
    }

    private String resolveProjectName() {
        String dir = new File(System.getProperty("user.dir")).getName();
        return dir.endsWith("-generator") ? dir.substring(0, dir.length() - "-generator".length()) : dir;
    }

    private String resolveTableName(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--table=")) {
                return arg.substring("--table=".length()).trim();
            }
        }
        if (props.getTable() != null && !props.getTable().isEmpty()) {
            return props.getTable();
        }
        return readTableName();
    }

    private void generateInfrastructure(String table, String infraJava, String infraRes) {
        String tablePkg = toPackageName(table);
        String basePackage = props.getBasePackage();

        Map<OutputFile, String> pathInfo = new HashMap<>();
        pathInfo.put(OutputFile.xml, infraRes + "/mapper/" + tablePkg);

        mkdirs(infraJava, infraRes, infraRes + "/mapper/" + tablePkg);

        FastAutoGenerator.create(dataSource)
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

        log.info("[infrastructure] Generated table: {}", table);
        log.info("  Entity -> {}/{}/infrastructure/{}/entity/", infraJava, basePackage.replace('.', '/'), tablePkg);
        log.info("  Mapper -> {}/{}/infrastructure/{}/mapper/", infraJava, basePackage.replace('.', '/'), tablePkg);
        log.info("  XML    -> {}/mapper/{}/", infraRes, tablePkg);
    }

    private void generateApp(String table, String appJava) {
        String tablePkg = toPackageName(table);
        String basePackage = props.getBasePackage();

        mkdirs(appJava);

        FastAutoGenerator.create(dataSource)
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

        log.info("[app] Generated table: {}", table);
        log.info("  IService    -> {}/{}/app/{}/", appJava, basePackage.replace('.', '/'), tablePkg);
        log.info("  ServiceImpl -> {}/{}/app/{}/impl/", appJava, basePackage.replace('.', '/'), tablePkg);
    }

    private void listTables() {
        System.out.println("====== Database Tables ======");
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                System.out.println("  " + rs.getString("TABLE_NAME"));
            }
        } catch (Exception e) {
            System.err.println("Query failed: " + e.getMessage());
        }
        System.out.println("=============================");
    }

    private String readTableName() {
        System.out.print("Enter table name: ");
        return new Scanner(System.in).nextLine().trim();
    }

    private String toPackageName(String tableName) {
        String lower = tableName.toLowerCase();
        for (String prefix : props.getTablePrefixes()) {
            if (lower.startsWith(prefix)) {
                lower = lower.substring(prefix.length());
                break;
            }
        }
        return lower.replace("_", "");
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
