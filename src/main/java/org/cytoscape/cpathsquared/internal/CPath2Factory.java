package org.cytoscape.cpathsquared.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cpathsquared.internal.view.BinarySifVisualStyleFactory;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import cpath.client.CPath2Client;
import cpath.client.util.CPathException;
import cpath.service.OutputFormat;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseResponse;

/** A "God" singleton object, which, once initialized, provides 
 *  access to injected Cy3 OSGi services and static constants 
 *  throughout all the implementation classes within this app.
 */
public final class CPath2Factory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CPath2Factory.class);
	
	private static CySwingApplication application;
	private static TaskManager taskManager;
	private static OpenBrowser openBrowser;
	private static CyNetworkManager networkManager;
	private static CyApplicationManager applicationManager;
	private static CyNetworkViewManager networkViewManager;
	private static CyNetworkReaderManager networkViewReaderManager;
	private static CyNetworkNaming naming;
	private static CyNetworkFactory networkFactory;
	private static CyLayoutAlgorithmManager layoutManager;
	private static UndoSupport undoSupport;
	private static BinarySifVisualStyleFactory binarySifVisualStyleUtil;
	private static VisualMappingManager mappingManager;
	private static CyProperty cyProperty;
	
	public static final String JVM_PROPERTY_CPATH2_URL = "cPath2Url";
	public static final String DEFAULT_CPATH2_URL = "http://www.pathwaycommons.org/pc2/";	
    public static final String cPathUrl = System.getProperty(JVM_PROPERTY_CPATH2_URL, DEFAULT_CPATH2_URL);   
    public static final String serverName = "Pathway Commons (BioPAX L3)";
    public static final String INFO_ABOUT = 
    	"<span class='bold'>Pathway Commons 2</span> is a warehouse of " +
    	"biological pathway information integrated from public databases and " +
    	"persisted in BioPAX Level3 format, which one can search, traverse, download.";
    
    public static String iconToolTip  = "Import Pathway Data from Pathway Commons (cPathSquared web services, BioPAX L3)";
    
    public static String iconFileName = "pc.png";
    
    public static OutputFormat downloadMode = OutputFormat.BINARY_SIF;
    
    public static enum SearchFor {
    	PATHWAY,
    	INTERACTION,
    	PHYSICALENTITY;
    }

    public static SearchFor searchFor = SearchFor.INTERACTION;    
    
	// non-instantiable static factory class
	private CPath2Factory() {
		throw new AssertionError();
	}
	
	public static void init(CySwingApplication app, TaskManager tm, OpenBrowser ob, 
			CyNetworkManager nm, CyApplicationManager am, CyNetworkViewManager nvm, 
			CyNetworkReaderManager nvrm, CyNetworkNaming nn, CyNetworkFactory nf, 
			CyLayoutAlgorithmManager lam, UndoSupport us, 
			BinarySifVisualStyleFactory bsvsf, VisualMappingManager mm,
			CyProperty prop) 
	{
		application = app;
		taskManager = tm;
		openBrowser = ob;
		networkManager = nm;
		applicationManager = am;
		networkViewManager = nvm;
		networkViewReaderManager = nvrm;
		naming = nn;
		layoutManager = lam;
		networkFactory = nf;
		undoSupport = us;
		binarySifVisualStyleUtil = bsvsf;
		mappingManager = mm;
		cyProperty = prop;
	}

	public static OpenBrowser getOpenBrowser() {
		return openBrowser;
	}

	public static CySwingApplication getCySwingApplication() {
		return application;
	}

	public static TaskManager getTaskManager() {
		return taskManager;
	}

	public static CyNetworkManager getNetworkManager() {
		return networkManager;
	}

	public static CyApplicationManager getCyApplicationManager() {
		return applicationManager;
	}

	public static CyNetworkViewManager getCyNetworkViewManager() {
		return networkViewManager;
	}

	public static CyNetworkReaderManager getCyNetworkViewReaderManager() {
		return networkViewReaderManager;
	}

	public static CyNetworkNaming getCyNetworkNaming() {
		return naming;
	}

	public static CyNetworkFactory getCyNetworkFactory() {
		return networkFactory;
	}

	public static UndoSupport getUndoSupport() {
		return undoSupport;
	}

	public static CyNetworkManager getCyNetworkManager() {
		return networkManager;
	}

	public static CyLayoutAlgorithmManager getCyLayoutAlgorithmManager() {
		return layoutManager;
	}
	
	public static BinarySifVisualStyleFactory getBinarySifVisualStyleUtil() {
		return binarySifVisualStyleUtil;
	}

	public static CyNetworkViewManager getNetworkViewManager() {
		return networkViewManager;
	}

	public static CyNetworkReaderManager getNetworkViewReaderManager() {
		return networkViewReaderManager;
	}

	public static CyNetworkNaming getNaming() {
		return naming;
	}

	public static CyNetworkFactory getNetworkFactory() {
		return networkFactory;
	}

	public static CyLayoutAlgorithmManager getLayoutManager() {
		return layoutManager;
	}

	public static VisualMappingManager getMappingManager() {
		return mappingManager;
	}
    
    public static CPath2Client newClient() {
        CPath2Client client = CPath2Client.newInstance();
        client.setEndPointURL(cPathUrl);
		return client;
	}

	/**
     * Gets One or more records by Primary ID.
     * @param ids               Array of URIs.
     * @param format            Output format.
     * @return data string.
     * @throws EmptySetException    Empty Set Error.
     */
    public static String getRecordsByIds(String[] ids, OutputFormat format) 
    {
    	//TODO client to return other formats as well
    	CPath2Client cli = newClient();
//    	Model res = cli.get(Arrays.asList(ids));  
//    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        BioPaxUtil.getBiopaxIO().convertToOWL(res, baos);
//        
//        return baos.toString();
    	
    	String queryUrl = cli.queryGet(Arrays.asList(ids));
    	RestTemplate template = new RestTemplate();
    	
    	return template.getForObject(queryUrl, String.class);
    }


    public static Map<String, String> getAvailableOrganisms() {
        return newClient().getValidOrganisms();
    }

    
    public static Map<String, String> getLoadedDataSources() {
        return newClient().getValidDataSources();
    }


	public static SearchResponse topPathways(String keyword, Set<String> organism,
			Set<String> datasource) {
		return newClient().getTopPathways();
	}
	
	
    public static TraverseResponse traverse(String path, Collection<String> uris) 
    {
    	if(LOGGER.isDebugEnabled())
    		LOGGER.debug("traverse: path=" + path);

        CPath2Client client = newClient();
        client.setPath(path);
        
        TraverseResponse res = null;
		try {
			res = client.traverse(uris);;
		} catch (CPathException e) {
			LOGGER.error("getting " + path + 
				" failed; uri:" + uris.toString(), e);
		}
			
       	return res;
    }

    
    public static SearchResponse unmodifiableSearchResponce(final SearchResponse resp) {
    	if(resp == null)
    		return null;
    	
    	// create a read-only proxy/view
    	SearchResponse imm = new SearchResponse() {
			@Override
			public String getComment() {
				return resp.getComment();
			}

			@Override
			public Integer getMaxHitsPerPage() {
				return resp.getMaxHitsPerPage();
			}

			@Override
			public Integer getNumHits() {
				return resp.getNumHits();
			}

			@Override
			public Integer getPageNo() {
				return resp.getPageNo();
			}

			@Override
			public List<SearchHit> getSearchHit() {
				return Collections.unmodifiableList(resp.getSearchHit());
			}

			@Override
			public boolean isEmpty() {
				return resp.isEmpty();
			}

			@Override
			public void setComment(String comment) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setMaxHitsPerPage(Integer maxHitsPerPage) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setNumHits(Integer numHits) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setPageNo(Integer pageNo) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void setSearchHit(List<SearchHit> searchHit) {
				throw new UnsupportedOperationException();
			}
    	};  	
    	
    	return imm;
    }
    
    
    /**
     * Gets a global Cytoscape property value. 
     * 
     * @param key
     * @return
     */
    public static Object getCyProperty(String key) {
		return ((Properties)cyProperty.getProperties()).getProperty(key);
	}
}
