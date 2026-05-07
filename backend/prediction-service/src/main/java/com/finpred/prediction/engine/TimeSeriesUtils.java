package com.finpred.prediction.engine;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Utilitários compartilhados para análise de séries temporais.
 * Fornece funções estatísticas e de pré-processamento usadas por todos
 * os algoritmos de previsão (ARMA, Holt-Winters, Regressão).
 */
public final class TimeSeriesUtils {

    private TimeSeriesUtils() {
        // Utility class — prevent instantiation
    }

    /**
     * Calcula o MAPE (Mean Absolute Percentage Error).
     * 
     * MAPE = (100% / n) * Σ |Ai - Fi| / |Ai|
     * 
     * Valores reais iguais a zero são ignorados para evitar divisão por zero.
     *
     * @param actual    Valores reais observados
     * @param predicted Valores previstos pelo modelo
     * @return MAPE em porcentagem (ex: 12.5 para 12.5%)
     */
    public static double calculateMAPE(double[] actual, double[] predicted) {
        if (actual.length != predicted.length || actual.length == 0) {
            return 100.0; // Sem dados válidos → erro máximo
        }

        double sumAPE = 0.0;
        int validCount = 0;

        for (int i = 0; i < actual.length; i++) {
            if (Math.abs(actual[i]) > 1e-10) { // Evita divisão por zero
                sumAPE += Math.abs((actual[i] - predicted[i]) / actual[i]);
                validCount++;
            }
        }

        if (validCount == 0) return 100.0;
        return (sumAPE / validCount) * 100.0;
    }

    /**
     * Calcula o RMSE (Root Mean Square Error).
     *
     * RMSE = √(Σ(Ai - Fi)² / n)
     *
     * @param actual    Valores reais observados
     * @param predicted Valores previstos
     * @return RMSE absoluto
     */
    public static double calculateRMSE(double[] actual, double[] predicted) {
        if (actual.length != predicted.length || actual.length == 0) {
            return Double.MAX_VALUE;
        }

        double sumSquaredErrors = 0.0;
        for (int i = 0; i < actual.length; i++) {
            double error = actual[i] - predicted[i];
            sumSquaredErrors += error * error;
        }

        return Math.sqrt(sumSquaredErrors / actual.length);
    }

    /**
     * Calcula índices sazonais para uma série temporal.
     *
     * Para cada posição no ciclo (0 a period-1), calcula a razão entre
     * a média dos valores naquela posição e a média geral da série.
     *
     * Um índice > 1.0 indica período acima da média (ex: Dezembro = 1.5).
     * Um índice < 1.0 indica período abaixo da média (ex: Janeiro = 0.9).
     *
     * @param data   Série temporal (ex: receita mensal de 12+ meses)
     * @param period Comprimento do ciclo sazonal (12 para dados mensais)
     * @return Array de índices sazonais com tamanho = period
     */
    public static double[] calculateSeasonalIndices(double[] data, int period) {
        if (data.length < period) {
            // Dados insuficientes para calcular sazonalidade — retorna índices neutros
            double[] neutral = new double[period];
            java.util.Arrays.fill(neutral, 1.0);
            return neutral;
        }

        double[] seasonalSums = new double[period];
        int[] seasonalCounts = new int[period];

        for (int i = 0; i < data.length; i++) {
            int seasonIndex = i % period;
            seasonalSums[seasonIndex] += data[i];
            seasonalCounts[seasonIndex]++;
        }

        // Média geral
        double overallMean = 0.0;
        for (double value : data) {
            overallMean += value;
        }
        overallMean /= data.length;

        // Índices sazonais: média_do_período / média_geral
        double[] indices = new double[period];
        for (int i = 0; i < period; i++) {
            double seasonalMean = seasonalCounts[i] > 0
                    ? seasonalSums[i] / seasonalCounts[i]
                    : overallMean;

            indices[i] = overallMean > 1e-10
                    ? seasonalMean / overallMean
                    : 1.0;
        }

        return indices;
    }

    /**
     * Remove a tendência linear de uma série temporal.
     *
     * Ajusta uma regressão linear simples (y = a + bx) e subtrai
     * os valores estimados da série original, retornando os resíduos.
     *
     * @param data Série temporal original
     * @return Série sem tendência (resíduos)
     */
    public static double[] detrend(double[] data) {
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < data.length; i++) {
            regression.addData(i, data[i]);
        }

        double[] detrended = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            detrended[i] = data[i] - regression.predict(i);
        }

        return detrended;
    }

    /**
     * Aplica diferenciação de ordem d a uma série temporal.
     *
     * A diferenciação é usada para tornar uma série estacionária,
     * removendo tendência e/ou sazonalidade.
     *
     * Para ordem 1: diff[i] = data[i] - data[i-1]
     * Para ordem 2: aplica diferenciação 1 duas vezes.
     *
     * @param data  Série temporal original
     * @param order Ordem da diferenciação (1 ou 2)
     * @return Série diferenciada (tamanho reduzido em 'order')
     */
    public static double[] difference(double[] data, int order) {
        double[] result = data.clone();

        for (int d = 0; d < order; d++) {
            double[] diffed = new double[result.length - 1];
            for (int i = 1; i < result.length; i++) {
                diffed[i - 1] = result[i] - result[i - 1];
            }
            result = diffed;
        }

        return result;
    }

    /**
     * Teste simplificado de estacionariedade baseado em variância.
     *
     * Divide a série em duas metades e compara a variância de cada uma.
     * Se a razão entre variâncias é próxima de 1.0 (±50%), a série é
     * considerada estacionária.
     *
     * Este não é um teste formal (como Dickey-Fuller), mas é suficiente
     * para determinar se diferenciação é necessária antes de aplicar ARMA.
     *
     * @param data Série temporal
     * @return true se a série parece estacionária
     */
    public static boolean isStationary(double[] data) {
        if (data.length < 6) return false; // Insuficiente para testar

        int half = data.length / 2;

        double var1 = variance(data, 0, half);
        double var2 = variance(data, half, data.length);

        if (var1 < 1e-10 || var2 < 1e-10) return true; // Série constante

        double ratio = var1 / var2;
        // Se a razão de variâncias está entre 0.5 e 2.0, é "estacionária o suficiente"
        return ratio >= 0.5 && ratio <= 2.0;
    }

    /**
     * Calcula a variância de um segmento da série.
     */
    private static double variance(double[] data, int start, int end) {
        int n = end - start;
        if (n <= 1) return 0.0;

        double mean = 0.0;
        for (int i = start; i < end; i++) {
            mean += data[i];
        }
        mean /= n;

        double sumSq = 0.0;
        for (int i = start; i < end; i++) {
            double diff = data[i] - mean;
            sumSq += diff * diff;
        }

        return sumSq / (n - 1);
    }

    /**
     * Calcula o fator de correção automático baseado no viés médio.
     *
     * F_correção = 1 + Média(Ai - Fi) / |Média(Fi)|
     *
     * Se o modelo subestima sistematicamente, o fator será > 1.0.
     * Se o modelo superestima, será < 1.0.
     *
     * O fator é limitado ao intervalo [0.5, 1.5] para evitar correções
     * excessivas que poderiam desestabilizar o modelo.
     *
     * @param actual    Valores reais observados
     * @param predicted Valores previstos
     * @return Fator de correção multiplicativo
     */
    public static double calculateCorrectionFactor(double[] actual, double[] predicted) {
        if (actual.length != predicted.length || actual.length == 0) {
            return 1.0; // Sem dados → sem correção
        }

        double sumErrors = 0.0;
        double sumPredicted = 0.0;

        for (int i = 0; i < actual.length; i++) {
            sumErrors += (actual[i] - predicted[i]);
            sumPredicted += predicted[i];
        }

        double meanError = sumErrors / actual.length;
        double absMeanPredicted = Math.abs(sumPredicted / actual.length);

        if (absMeanPredicted < 1e-10) return 1.0;

        double factor = 1.0 + (meanError / absMeanPredicted);

        // Clamp entre 0.5 e 1.5 para segurança
        return Math.max(0.5, Math.min(1.5, factor));
    }

    /**
     * Classifica a qualidade do modelo baseado no MAPE.
     *
     * @param mape Valor do MAPE em porcentagem
     * @return Classificação textual
     */
    public static String classifyMAPE(double mape) {
        if (mape < 10.0) return "ALTAMENTE_PRECISO";
        if (mape < 20.0) return "BOM";
        if (mape < 50.0) return "RAZOAVEL";
        return "IMPRECISO";
    }

    /**
     * Converte a classificação MAPE para exibição em português.
     */
    public static String classifyMAPEDisplay(double mape) {
        if (mape < 10.0) return "Altamente Preciso";
        if (mape < 20.0) return "Bom";
        if (mape < 50.0) return "Razoável";
        return "Impreciso — requer mais dados";
    }
}
