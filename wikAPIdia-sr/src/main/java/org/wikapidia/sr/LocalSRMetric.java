package org.wikapidia.sr;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */

public interface LocalSRMetric {

    /**
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();

    /**
     * Determine the similarity between two local pages.
     *
     * @param page1 The first page.
     * @param page2 The second page.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException;

    /**
     * Determine the similarity between two strings in a given language by mapping through local pages.
     *
     * @param phrase1 The first phrase.
     * @param phrase2 The second phrase.
     * @param language The language of the phrases.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException;

    /**
     * Find the most similar local pages to a local page within the same language.
     *
     * @param page The local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException;

    /**
     * Find the most similar local pages to a local page.
     *
     * @param page The local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds The local page ids to be considered.  Null means all ids in the language.
     * @return
     */
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException;

    /**
     * Find the most similar local pages to a phrase.
     *
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws DaoException;

    /**
     * Find the most similar local pages to a phrase.
     *
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds The local page ids to be considered.  Null means all ids in the language
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException;

    /**
     * Writes the metric to a directory.
     *
     * @param path A directory data will be written to.
     *                  Any existing data in the directory may be destroyed.
     * @throws java.io.IOException
     */
    public void write(String path) throws IOException;

    /**
     * Reads the metric from a directory.
     *
     * @param path A directory data will be read from.
     *                  The directory previously will have been written to by write().
     * @throws IOException if the file is not found or is unusable
     */
    public void read(String path) throws IOException;

    /**
     * Train the similarity() function.
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset
     */
    public void trainDefaultSimilarity(Dataset dataset) throws DaoException;

    /**
     * Train the mostSimilar() function
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset.
     * @param numResults The maximum number of similar articles computed per phrase.
     * @param validIds The Wikipedia ids that should be considered in result sets. Null means all ids.
     */
    public void trainDefaultMostSimilar(Dataset dataset, int numResults, TIntSet validIds);

    /**
     * Train the similarity() function.
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset
     */
    public void trainSimilarity(Dataset dataset) throws DaoException;

    /**
     * Train the mostSimilar() function
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset.
     * @param numResults The maximum number of similar articles computed per phrase.
     * @param validIds The Wikipedia ids that should be considered in result sets. Null means all ids.
     */
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds);


    public void setDefaultMostSimilarNormalizer(Normalizer n);

    public void setDefaultSimilarityNormalizer(Normalizer defaultSimilarityNormalizer);

    public void setMostSimilarNormalizer(Normalizer n, Language l);

    public void setSimilarityNormalizer(Normalizer n, Language l);

    /**
     * Return a vector representation of a LocalPage
     * @param id Local id of the page to be described.
     * @param language Language of the page to be described
     * @return A vector of a page's scores versus all other pages
     */
    public TIntDoubleMap getVector(int id, Language language) throws DaoException;


    /**
     * Construct a cosimilarity matrix of Wikipedia ids in a given language.
     *
     * @param wpRowIds
     * @param wpColIds
     * @param language The language of the pages.
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(int wpRowIds[], int wpColIds[], Language language) throws DaoException;


    /**
     * Construct a cosimilarity matrix of phrases.
     *
     * @param rowPhrases
     * @param colPhrases
     * @param language The language of the phrases.
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[], Language language) throws DaoException;

    /**
     * Construct symmetric comsimilarity matrix of Wikipedia ids in a given language.
     *
     * @param ids
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(int ids[], Language language) throws DaoException;

    /**
     * Construct symmetric cosimilarity matrix of phrases by mapping through local pages.
     *
     * @param phrases
     * @param language The language of the phrases.
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(String phrases[], Language language) throws DaoException;

    /**
     * Writes a cosimilarity matrix to file based off of the getVector function and pairwise cosine similarity class
     * @param path the directory to write the matrix in
     * @param languages the set of languages that you would like matrices for
     * @param maxHits the number of document hits you would like returned from the most similar function
     */
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException;

    public void readCosimilarity(String path, LanguageSet languages) throws IOException;
}