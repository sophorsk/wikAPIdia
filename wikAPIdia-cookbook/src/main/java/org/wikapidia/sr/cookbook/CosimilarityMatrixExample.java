package org.wikapidia.sr.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.UniversalSRMetric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class CosimilarityMatrixExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, WikapidiaException, InterruptedException, IOException {
        //Set-up
        Configurator c = new Configurator(new Configuration());
        LocalSRMetric sr = c.get(LocalSRMetric.class);
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        UniversalPageDao universalPageDao = c.get(UniversalPageDao.class);
        String path = c.getConf().get().getString("sr.metric.path");

        Language language = Language.getByLangCode("simple");
        LanguageSet languages = new LanguageSet("simple");
        sr.writeCosimilarity(path,languages, 100);
//        UniversalSRMetric usr = c.get(UniversalSRMetric.class);
//        usr.writeCosimilarity(path,100);

        List<LocalString> phrases = new ArrayList<LocalString>();
        phrases.add(new LocalString(language, "United States"));
        phrases.add(new LocalString(language, "Barack Obama"));
        phrases.add(new LocalString(language, "brain"));
        phrases.add(new LocalString(language, "natural language processing"));

        for (LocalString phrase : phrases){
            System.out.println("\nMost similar to "+phrase.getString()+":");
            SRResultList results = sr.mostSimilar(phrase, 5);
            for (int i=0; i<results.numDocs(); i++){
                LocalPage page = localPageDao.getById(language,results.get(i).getId());
                String name = page.getTitle().getCanonicalTitle();
                System.out.println("#"+(i+1)+" "+name);
            }
        }

//        for (LocalString phrase : phrases){
//            System.out.println("\nMost similar to "+phrase.getString()+":");
//            SRResultList results = usr.mostSimilar(phrase, 5);
//            for (int i=0; i<results.numDocs(); i++){
//                UniversalPage page = universalPageDao.getById(results.get(i).getId(),usr.getAlgorithmId());
//                LocalId nameId = (LocalId) page.getLocalIds(page.getLanguageSet().getDefaultLanguage()).toArray()[0];
//                LocalPage namePage = localPageDao.getById(nameId.getLanguage(),nameId.getId());
//                String name = namePage.getTitle().getCanonicalTitle();
//                System.out.println("#"+(i+1)+" "+name);
//            }
//        }

    }
}
