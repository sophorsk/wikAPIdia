package org.wikapidia.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.evaluation.CrossValidation;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.DatasetDao;
import org.wikapidia.sr.utils.ExplanationFormatter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class LocalSRMetricIT {
    private static final Language SIMPLE = Language.getByLangCode("simple");

    // Set this to true
    private static final Boolean SKIP_RESTORE = true;

    @BeforeClass
    public static void prepareDump() throws ConfigurationException, IOException, SQLException {
        if (SKIP_RESTORE) {
            return;
        }
        TestDB db = TestUtils.getTestDb();
        db.restoreLucene();
    }

    @Test
    public void testESAAccuracy() throws Exception {
        testAccuracy("ESA", 0.50, 0.58, 0);
        testExplain("ESA", "President", "Obama");
    }

    @Test
    public void testMilneWittenAccuracy() throws Exception {
        testAccuracy("milnewitten", 0.35, 0.37, 0);
        testExplain("milnewitten", "President", "Obama");
    }

    public void testAccuracy(String srName, double minPearson, double minSpearman, int maxNoPred) throws ConfigurationException, DaoException {
        Env env = TestUtils.getEnv();
        LocalSRMetric sr = env.getConfigurator().get(LocalSRMetric.class, srName);
        DatasetDao datasetDao = new DatasetDao();
        String datasetPath = env.getConfiguration().get().getString("sr.dataset.path");
        datasetPath = datasetPath.replace("integration-tests/", "");
        Dataset ds = datasetDao.read(SIMPLE, new File(datasetPath, "wordsim353.txt").toString());
        CrossValidation cv = new CrossValidation();

        List<Dataset> allTrain = new ArrayList<Dataset>();
        List<Dataset> allTest = new ArrayList<Dataset>();
        CrossValidation.makeFolds(ds.split(7), allTrain, allTest);
        for (int i = 0; i < allTrain.size(); i++) {
            sr.trainDefaultSimilarity(allTrain.get(i));
            sr.trainSimilarity(allTrain.get(i));
            cv.evaluate(sr, allTest.get(i));
        }
        System.out.println("results for " + srName);
        System.out.println("\tpearson: " + cv.getPearson());
        System.out.println("\tspearman: " + cv.getSpearman());
        System.out.println("\tmissing: " + cv.getMissing());
        System.out.println("\tfailed: " + cv.getFailed());
        assertTrue(cv.getPearson() >= minPearson);
        assertTrue(cv.getSpearman() >= minSpearman);
        assertTrue(cv.getMissing() + cv.getFailed() <= maxNoPred);
    }

    public void testExplain(String srName, String phrase1, String phrase2) throws ConfigurationException, DaoException {
        Env env = TestUtils.getEnv();
        DatasetDao datasetDao = new DatasetDao();
        String datasetPath = env.getConfiguration().get().getString("sr.dataset.path");
        datasetPath = datasetPath.replace("integration-tests/", "");
        Dataset ds = datasetDao.read(SIMPLE, new File(datasetPath, "wordsim353.txt").toString());
        LocalSRMetric sr = env.getConfigurator().get(LocalSRMetric.class, srName);
        sr.trainDefaultSimilarity(ds);
        sr.trainSimilarity(ds);
        ExplanationFormatter formatter = env.getConfigurator().get(ExplanationFormatter.class);
        SRResult result = sr.similarity(phrase1, phrase2, SIMPLE, true);
        System.out.println(srName + " explanation for " + phrase1 + ", " + phrase2 + " is:");
        assertNotNull(result.getExplanations());
        for (Explanation ex : result.getExplanations()) {
            System.out.println("\t" + formatter.formatExplanation(ex));
        }
    }
}
