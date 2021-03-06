package org.wikapidia.core.model;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;

/**
 */
public class LocalCategoryMember {
    private final int categoryId;
    private final int articleId;
    private final Language language;

    public LocalCategoryMember(LocalCategory localCategory, LocalArticle localArticle) throws WikapidiaException {
        if (!localArticle.getLanguage().equals(localCategory.getLanguage())) {
            throw new WikapidiaException("Language Mismatch");
        }
        this.categoryId = localCategory.getLocalId();
        this.articleId = localArticle.getLocalId();
        this.language = localCategory.getLanguage();
    }

    public LocalCategoryMember(int categoryId, int articleId, Language language) {
        this.categoryId = categoryId;
        this.articleId = articleId;
        this.language = language;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public int getArticleId() {
        return articleId;
    }

    public Language getLanguage() {
        return language;
    }
}
