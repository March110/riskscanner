package io.riskscanner.commons.utils;

import org.apache.commons.exec.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

/**
 * @author maguohao
 */
public class CommandUtils {

    /**
     * @param command
     * @param workDir 工作路径
     * @throws Exception
     */
    public static String commonExecCmdWithResult(String command, String workDir) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        Process exec;
        if (StringUtils.isNotBlank(workDir)) {
            exec = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command}, null, new File(workDir));
        } else {
            exec = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
        }
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(exec.getErrorStream()))
        ) {
            exec.waitFor();
            String line;
            if (exec.exitValue() != 0) {
                //错误执行返回信息
                while ((line = errorReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
            } else {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
            }
        } catch (InterruptedException e) {
            throw e;
        }
        return stringBuilder.toString();
    }

    public static String saveAsFile(String content, String dirPath, String fileName) throws Exception {
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileWriter fwriter = null;
        try {
            // true表示不覆盖原来的内容，而是加到文件的后面。若要覆盖原来的内容，直接省略这个参数就好
            fwriter = new FileWriter(dirPath + "/" + fileName, false);
            fwriter.write(content);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                assert fwriter != null;
                fwriter.flush();
                fwriter.close();
            } catch (IOException e) {
                throw e;
            } finally {
                // empty!
            }
        }
        return dirPath;
    }

    /**
     * @param command
     * @param workDir 工作路径
     * @Desc Nuclei 调用命令行工具，Java 调用 Runtime 执行 nuclei 命令会阻塞，所以找了apache-commons-exec异步执行
     * @throws Exception
     */
    public static String commonExecCmdWithResultByNuclei(String command, String workDir) throws Exception {
        FileOutputStream fileOutputStream = null;
        try {
            // 命令行
            CommandLine commandLine = CommandLine.parse(command);

            // 重定向stdout和stderr到文件
            fileOutputStream = new FileOutputStream(workDir + "/exec.log");
            PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(fileOutputStream);

            // 创建执行器
            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

            ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
            Executor executor = new DefaultExecutor();
            executor.setStreamHandler(pumpStreamHandler);
            executor.setExitValue(1);
            executor.setWatchdog(watchdog);
            executor.execute(commandLine, resultHandler);

            resultHandler.waitFor();

            return ReadFileUtils.readToBuffer(workDir + "/exec.log");
        } catch (Exception e) {
            throw e;
        } finally {
            // 关闭流
            fileOutputStream.close();
        }

    }

}
