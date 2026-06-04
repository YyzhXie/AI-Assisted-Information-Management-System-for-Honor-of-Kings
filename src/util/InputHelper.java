package util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class InputHelper {
    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("输入不能为空。");
        }
    }

    public String readOptional(String prompt, String currentValue) {
        System.out.print(prompt + "（回车保持：" + currentValue + "）: ");
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? currentValue : value;
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("请输入 " + min + " 到 " + max + " 之间的数字。");
            } catch (NumberFormatException ex) {
                System.out.println("请输入有效整数。");
            }
        }
    }

    public double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(raw);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("请输入 " + min + " 到 " + max + " 之间的数字。");
            } catch (NumberFormatException ex) {
                System.out.println("请输入有效数字。");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException ex) {
                System.out.println("日期格式无效，请使用 yyyy-MM-dd，例如 2026-06-04。");
            }
        }
    }
}
