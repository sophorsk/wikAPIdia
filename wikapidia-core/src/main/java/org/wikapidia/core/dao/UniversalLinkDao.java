package org.wikapidia.core.dao;

import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.UniversalLink;

/**
 */
public interface UniversalLinkDao extends Loader<UniversalLink>{

    public abstract void save(LocalLink localLink, int sourceUnivId, int destUnivId, int algorithmId) throws DaoException;

    public abstract Iterable<UniversalLink> getBySourceId(int sourceId, int algorithmId) throws DaoException;

    public abstract Iterable<UniversalLink> getByDestId(int destId, int algorithmId) throws DaoException;

    public abstract UniversalLink getUniversalLink(int sourceId, int destId, int algorithmId) throws DaoException;
}