package org.example.models;

public class ModelInitializer {
    private static volatile MahoutIncrementalModel globalModel;
    private static final Object lock = new Object();

    public static MahoutIncrementalModel getModel() {
        if (globalModel == null) {
            synchronized (lock) {
                if (globalModel == null) {
                    globalModel = new MahoutIncrementalModel();
                    checkModelStatus();
                }
            }
        }
        return globalModel;
    }

    private static void checkModelStatus() {
        if (!globalModel.hasEnoughDataForRecommendations()) {
            System.out.println("РЕЖИМ НАКОПЛЕНИЯ ДАННЫХ");
            System.out.println(globalModel.getCurrentStats());
        } else if (!globalModel.isInitialized()) {
            try {
                System.out.println("ПЕРВОНАЧАЛЬНОЕ ОБУЧЕНИЕ МОДЕЛИ");
                globalModel.trainGlobal();
            } catch (Exception e) {
                System.err.println("Ошибка обучения: " + e.getMessage());
            }
        } else {
            System.out.println("МОДЕЛЬ ЗАГРУЖЕНА");
            System.out.println(globalModel.getCurrentStats());
        }
    }

    public static void shutdown() {
        if (globalModel != null) {
            globalModel.shutdown();
        }
    }
}