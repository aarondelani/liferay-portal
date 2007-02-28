/**
 * Copyright (c) 2000-2007 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.softwarecatalog.service.impl;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.lucene.LuceneFields;
import com.liferay.portal.lucene.LuceneUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.model.impl.ResourceImpl;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.persistence.UserUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.softwarecatalog.ProductEntryLicenseException;
import com.liferay.portlet.softwarecatalog.ProductEntryNameException;
import com.liferay.portlet.softwarecatalog.ProductEntryShortDescriptionException;
import com.liferay.portlet.softwarecatalog.ProductEntryTypeException;
import com.liferay.portlet.softwarecatalog.model.SCProductEntry;
import com.liferay.portlet.softwarecatalog.service.SCProductVersionLocalServiceUtil;
import com.liferay.portlet.softwarecatalog.service.base.SCProductEntryLocalServiceBaseImpl;
import com.liferay.portlet.softwarecatalog.service.persistence.SCProductEntryUtil;
import com.liferay.portlet.softwarecatalog.util.Indexer;
import com.liferay.util.Validator;
import com.liferay.util.lucene.HitsImpl;

import java.io.IOException;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Searcher;

/**
 * <a href="SCProductEntryLocalServiceImpl.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author Jorge Ferrer
 * @author Brian Wing Shun Chan
 *
 */
public class SCProductEntryLocalServiceImpl
	extends SCProductEntryLocalServiceBaseImpl {

	public SCProductEntry addProductEntry(
			String userId, String plid, String name, String type,
			String shortDescription, String longDescription, String pageURL,
			String repoGroupId, String repoArtifactId, long[] licenseIds,
			boolean addCommunityPermissions, boolean addGuestPermissions)
		throws PortalException, SystemException {

		return addProductEntry(
			userId, plid, name, type, shortDescription, longDescription,
			pageURL, repoGroupId, repoArtifactId, licenseIds,
			new Boolean(addCommunityPermissions),
			new Boolean(addGuestPermissions), null, null);
	}

	public SCProductEntry addProductEntry(
			String userId, String plid, String name, String type,
			String shortDescription, String longDescription, String pageURL,
			String repoGroupId, String repoArtifactId, long[] licenseIds,
			String[] communityPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		return addProductEntry(
			userId, plid, name, type, shortDescription, longDescription,
			pageURL, repoGroupId, repoArtifactId, licenseIds, null, null,
			communityPermissions, guestPermissions);
	}

	public SCProductEntry addProductEntry(
			String userId, String plid, String name, String type,
			String shortDescription, String longDescription, String pageURL,
			String repoGroupId, String repoArtifactId, long[] licenseIds,
			Boolean addCommunityPermissions, Boolean addGuestPermissions,
			String[] communityPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		validate(name, type, shortDescription, licenseIds);

		// Product entry

		User user = UserUtil.findByPrimaryKey(userId);
		long groupId = PortalUtil.getPortletGroupId(plid);
		Date now = new Date();

		long productEntryId = CounterLocalServiceUtil.increment(
			SCProductEntry.class.getName());

		SCProductEntry productEntry = SCProductEntryUtil.create(productEntryId);

		productEntry.setGroupId(groupId);
		productEntry.setCompanyId(user.getCompanyId());
		productEntry.setUserId(user.getUserId());
		productEntry.setUserName(user.getFullName());
		productEntry.setCreateDate(now);
		productEntry.setModifiedDate(now);
		productEntry.setName(name);
		productEntry.setType(type);
		productEntry.setShortDescription(shortDescription);
		productEntry.setLongDescription(longDescription);
		productEntry.setPageURL(pageURL);
		productEntry.setRepoGroupId(repoGroupId);
		productEntry.setRepoArtifactId(repoArtifactId);

		SCProductEntryUtil.update(productEntry);

		// Resources

		if ((addCommunityPermissions != null) &&
			(addGuestPermissions != null)) {

			addProductEntryResources(
				productEntry, addCommunityPermissions.booleanValue(),
				addGuestPermissions.booleanValue());
		}
		else {
			addProductEntryResources(
				productEntry, communityPermissions, guestPermissions);
		}

		// Licenses

		SCProductEntryUtil.setSCLicenses(productEntryId, licenseIds);

		// Lucene

		try {
			Indexer.addProductEntry(
				productEntry.getCompanyId(), groupId, userId,
				user.getFullName(), productEntryId, name, type,
				shortDescription, longDescription, pageURL, repoGroupId,
				repoArtifactId);
		}
		catch (IOException ioe) {
			_log.error("Indexing " + productEntryId, ioe);
		}

		return productEntry;
	}

	public void addProductEntryResources(
			long productEntryId, boolean addCommunityPermissions,
			boolean addGuestPermissions)
		throws PortalException, SystemException {

		SCProductEntry productEntry =
			SCProductEntryUtil.findByPrimaryKey(productEntryId);

		addProductEntryResources(
			productEntry, addCommunityPermissions, addGuestPermissions);
	}

	public void addProductEntryResources(
			SCProductEntry productEntry, boolean addCommunityPermissions,
			boolean addGuestPermissions)
		throws PortalException, SystemException {

		ResourceLocalServiceUtil.addResources(
			productEntry.getCompanyId(), productEntry.getGroupId(),
			productEntry.getUserId(), SCProductEntry.class.getName(),
			productEntry.getPrimaryKey(), false, addCommunityPermissions,
			addGuestPermissions);
	}

	public void addProductEntryResources(
			long productEntryId, String[] communityPermissions,
			String[] guestPermissions)
		throws PortalException, SystemException {

		SCProductEntry productEntry =
			SCProductEntryUtil.findByPrimaryKey(productEntryId);

		addProductEntryResources(
			productEntry, communityPermissions, guestPermissions);
	}

	public void addProductEntryResources(
			SCProductEntry productEntry, String[] communityPermissions,
			String[] guestPermissions)
		throws PortalException, SystemException {

		ResourceLocalServiceUtil.addModelResources(
			productEntry.getCompanyId(), productEntry.getGroupId(),
			productEntry.getUserId(), SCProductEntry.class.getName(),
			productEntry.getPrimaryKey(), communityPermissions,
			guestPermissions);
	}

	public void deleteProductEntry(long productEntryId)
		throws PortalException, SystemException {

		SCProductEntry productEntry =
			SCProductEntryUtil.findByPrimaryKey(productEntryId);

		deleteProductEntry(productEntry);
	}

	public void deleteProductEntry(SCProductEntry productEntry)
		throws PortalException, SystemException {

		// Lucene

		try {
			Indexer.deleteProductEntry(
				productEntry.getCompanyId(), productEntry.getProductEntryId());
		}
		catch (IOException ioe) {
			_log.error(
				"Deleting index " + productEntry.getProductEntryId(), ioe);
		}

		// Product versions

		SCProductVersionLocalServiceUtil.deleteProductVersions(
			productEntry.getProductEntryId());

		// Resources

		ResourceLocalServiceUtil.deleteResource(
			productEntry.getCompanyId(), SCProductEntry.class.getName(),
			ResourceImpl.TYPE_CLASS, ResourceImpl.SCOPE_INDIVIDUAL,
			productEntry.getPrimaryKey());

		// Product entry

		SCProductEntryUtil.remove(productEntry.getProductEntryId());
	}

	public SCProductEntry getProductEntry(long productEntryId)
		throws PortalException, SystemException {

		return SCProductEntryUtil.findByPrimaryKey(productEntryId);
	}

	public List getProductEntries(long groupId, int begin, int end)
		throws SystemException {

		return SCProductEntryUtil.findByGroupId(groupId, begin, end);
	}

	public List getProductEntries(
			long groupId, String userId, int begin, int end)
		throws SystemException {

		return SCProductEntryUtil.findByG_U(groupId, userId, begin, end);
	}

	public int getProductEntriesCount(long groupId)
		throws SystemException {

		return SCProductEntryUtil.countByGroupId(groupId);
	}

	public int getProductEntriesCount(long groupId, String userId)
		throws SystemException {

		return SCProductEntryUtil.countByG_U(groupId, userId);
	}

	public void reIndex(String[] ids) throws SystemException {
		try {
			String companyId = ids[0];

			Iterator itr = SCProductEntryUtil.findByCompanyId(
				companyId).iterator();

			while (itr.hasNext()) {
				SCProductEntry productEntry = (SCProductEntry)itr.next();

				long productEntryId = productEntry.getProductEntryId();

				try {
					Indexer.addProductEntry(
						companyId, productEntry.getGroupId(),
						productEntry.getUserId(), productEntry.getUserName(),
						productEntryId, productEntry.getName(),
						productEntry.getType(),
						productEntry.getShortDescription(),
						productEntry.getLongDescription(),
						productEntry.getPageURL(),
						productEntry.getRepoGroupId(),
						productEntry.getRepoArtifactId());
				}
				catch (Exception e1) {
					_log.error("Reindexing " + productEntryId, e1);
				}
			}
		}
		catch (SystemException se) {
			throw se;
		}
		catch (Exception e2) {
			throw new SystemException(e2);
		}
	}

	public Hits search(
			String companyId, long groupId, String type, String keywords)
		throws SystemException {

		try {
			HitsImpl hits = new HitsImpl();

			if (Validator.isNull(type) && Validator.isNull(keywords)) {
				return hits;
			}

			BooleanQuery contextQuery = new BooleanQuery();

			LuceneUtil.addRequiredTerm(
				contextQuery, LuceneFields.PORTLET_ID, Indexer.PORTLET_ID);
			LuceneUtil.addRequiredTerm(
				contextQuery, LuceneFields.GROUP_ID, groupId);

			if (Validator.isNotNull(type)) {
				LuceneUtil.addRequiredTerm(contextQuery, "type", type);
			}

			BooleanQuery fullQuery = new BooleanQuery();

			fullQuery.add(contextQuery, BooleanClause.Occur.MUST);

			if (Validator.isNotNull(keywords)) {
				BooleanQuery searchQuery = new BooleanQuery();

				LuceneUtil.addTerm(searchQuery, LuceneFields.TITLE, keywords);
				LuceneUtil.addTerm(searchQuery, LuceneFields.CONTENT, keywords);

				fullQuery.add(searchQuery, BooleanClause.Occur.MUST);
			}

			Searcher searcher = LuceneUtil.getSearcher(companyId);

			hits.recordHits(searcher.search(fullQuery));

			return hits;
		}
		catch (IOException ioe) {
			throw new SystemException(ioe);
		}
		catch (ParseException pe) {
			_log.error("Parsing keywords " + keywords, pe);

			return new HitsImpl();
		}
	}

	public SCProductEntry updateProductEntry(
			long productEntryId, String name, String type,
			String shortDescription, String longDescription, String pageURL,
			String repoGroupId, String repoArtifactId, long[] licenseIds)
		throws PortalException, SystemException {

		validate(name, type, shortDescription, licenseIds);

		// Product entry

		SCProductEntry productEntry =
			SCProductEntryUtil.findByPrimaryKey(productEntryId);

		productEntry.setModifiedDate(new Date());
		productEntry.setName(name);
		productEntry.setType(type);
		productEntry.setShortDescription(shortDescription);
		productEntry.setLongDescription(longDescription);
		productEntry.setPageURL(pageURL);
		productEntry.setRepoGroupId(repoGroupId);
		productEntry.setRepoArtifactId(repoArtifactId);

		SCProductEntryUtil.update(productEntry);

		// Licenses

		SCProductEntryUtil.setSCLicenses(productEntryId, licenseIds);

		// Lucene

		try {
			Indexer.updateProductEntry(
				productEntry.getCompanyId(), productEntry.getGroupId(),
				productEntry.getUserId(), productEntry.getUserName(),
				productEntryId, name, type, shortDescription, longDescription,
				pageURL, repoGroupId, repoArtifactId);
		}
		catch (IOException ioe) {
			_log.error("Indexing " + productEntryId, ioe);
		}

		return productEntry;
	}

	protected void validate(
		String name, String type, String shortDescription, long[] licenseIds)
		throws PortalException {

		if (Validator.isNull(name)) {
			throw new ProductEntryNameException();
		}
		else if (Validator.isNull(type)) {
			throw new ProductEntryTypeException();
		}
		else if (Validator.isNull(shortDescription)) {
			throw new ProductEntryShortDescriptionException();
		}
		else if (licenseIds.length == 0) {
			throw new ProductEntryLicenseException();
		}
	}

	private static Log _log =
		LogFactory.getLog(SCProductEntryLocalServiceImpl.class);

}