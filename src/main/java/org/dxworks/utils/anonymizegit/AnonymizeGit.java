package org.dxworks.utils.anonymizegit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class AnonymizeGit {
    private static File createResultFolder(String resultFolderName) throws IOException {
        File directory = new File(resultFolderName);
        if (directory.exists()) Files.walk(directory.toPath()).map(Path::toFile).forEach(File::delete);
        directory.mkdirs();
        return directory;
    }

    private static List<String> getFilesOfType(String projectFolderName) throws IOException {
        if (!new File(projectFolderName).exists()) {
            System.err.println("Input folder does not exist: " + projectFolderName);
            System.err.println("You must put all Git log files in the folder provided as parameter!");
            System.exit(-1);
        }

        File[] files = new File(projectFolderName).listFiles((folder, name) -> !name.startsWith(".") && name.toLowerCase().endsWith(".git"));

        ArrayList<String> filenames = new ArrayList<>();

        if (files == null) {
            System.err.println("No files of filetype " + ".git" + " in folder: " + projectFolderName);
            return filenames;
        }

        for (File file : files) filenames.add(file.getCanonicalPath());

        return filenames;
    }


    static Character encrypt(Character car) {
        String consonants = "bcdfghjklmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ";
        String vowels = "aeiouAEIOU";

        if (car == 'u') return 'a';
        if (car == 'U') return 'A';
        if (car == 'z') return 'b';
        if (car == 'Z') return 'B';

        int indexOfCar = consonants.indexOf(car);

        if (indexOfCar == consonants.length() - 1) return consonants.charAt(0);
        if (indexOfCar >= 0) return consonants.charAt(indexOfCar + 1);

        indexOfCar = vowels.indexOf(car);
        if (indexOfCar == vowels.length() - 1) return vowels.charAt(0);
        if (indexOfCar >= 0) return vowels.charAt(indexOfCar + 1);

        return car;
    }


    private static String encryptLine(String name) {
        String encryptedName = name;

        List<Character> characters = name.chars().mapToObj(car -> Character.valueOf((char) car)).collect(Collectors.toList());

        encryptedName = characters.stream().map(car -> encrypt(car)).collect(Collector.of(
                StringBuilder::new,
                StringBuilder::append,
                StringBuilder::append,
                StringBuilder::toString));

        return encryptedName;
    }


    private static void processLogfile(String logFilename, String resultFoldername) {
        List<String> originalLines = new ArrayList<>();

        try {
            originalLines = java.nio.file.Files.readAllLines(Paths.get(logFilename));
        } catch (MalformedInputException e) {
            try {
                originalLines = java.nio.file.Files.readAllLines(Paths.get(logFilename), StandardCharsets.ISO_8859_1);
            } catch (IOException e1) {
                System.err.println("IO Exception while handing: " + logFilename);
            }
        } catch (NoSuchFileException e) {
            System.err.println("Log file does not exist: " + logFilename);
            System.exit(-100);
        } catch (IOException e) {
            System.err.println("IO Exception while handing: " + logFilename);
        }

        Pattern authorPattern = Pattern.compile("(author:)(.*)");
        Pattern emailPattern = Pattern.compile("(email:)(.*)(@.*)");

        ArrayList<String> resultingLines = new ArrayList<>();

        for (String crtLine : originalLines) {
            Matcher matcher = authorPattern.matcher(crtLine);
            if (matcher.find()) resultingLines.add(matcher.group(1) + encryptLine(matcher.group(2)));
            else {
                matcher = emailPattern.matcher(crtLine);
                if (matcher.find())
                    resultingLines.add(matcher.group(1) + encryptLine(matcher.group(2)) + matcher.group(3));
                else resultingLines.add(crtLine);
            }
        }

        System.err.println("Processed: " + logFilename + ": " + originalLines.size() + " lines => " + resultingLines.size());

        try {
            PrintWriter anonymisedFile = new PrintWriter(resultFoldername + File.separator + new File(logFilename).getName());
            resultingLines.forEach(anonymisedFile::println);
            anonymisedFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No input folder provided");
            System.err.println("You must put all Git log files in the folder provided as parameter!");
            System.exit(-1);
        }

        String baseFolder = args[0];

        try {
            getFilesOfType(baseFolder).forEach(logFilename -> processLogfile(logFilename, baseFolder));
        } catch (IOException e) {
            System.err.println("IOException");
        }
    }
}
