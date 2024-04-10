package edu.ucsb.nceas.metacat;

import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A utility class that encapsulates a node and its data
 */
public class NodeRecord {
  private long _nodeid = -1;
  private long _parentnodeid = -1;
  private long _nodeindex = -1;
  private String _nodename = null;
  private String _nodeprefix = null;
  private String _nodetype = null;
  private String _nodedata = null;
  private float _nodedatanumerical = -1;
  private Timestamp _nodedatadate = null;

  private Log logMetacat = LogFactory.getLog(NodeRecord.class);

  /**
   * Constructor
   */
  public NodeRecord(long nodeid, long parentnodeid, long nodeindex,
                    String nodetype, String nodename, String nodeprefix, 
                    String nodedata) {
	    setNodeId(nodeid);
	    setParentNodeId(parentnodeid);
	    setNodeIndex(nodeindex);
	    setNodeName(nodename);
	    setNodePrefix(nodeprefix);
	    setNodeType(nodetype);
	    setNodeData(nodedata);
  }
  
  public NodeRecord(long nodeid, long parentnodeid, long nodeindex, String nodetype,
			String nodename, String nodeprefix, String nodedata, float nodedatanumerical, Timestamp nodedatadate) {
		setNodeId(nodeid);
		setParentNodeId(parentnodeid);
		setNodeIndex(nodeindex);
		setNodeName(nodename);
		setNodePrefix(nodeprefix);
		setNodeType(nodetype);
		setNodeData(nodedata);
		setNodeDataNumerical(nodedatanumerical);
		setNodeDataDate(nodedatadate);
	}
  
  /** Get functions */
  public long getNodeId()
  {
    return _nodeid;
  }
  
  public long getParentNodeId()
  {
    return _parentnodeid;
  }
  
  public long getNodeIndex()
  {
    return _nodeindex;
  }
  
  public String getNodeName()
  {
    return _nodename;
  }
  
  public String getNodeType()
  {
    return _nodetype;
  }
  
  public String getNodePrefix()
  {
    return _nodeprefix;
  }
  
  public String getNodeData()
  {
    return _nodedata;
  }

  public float getNodeDataNumerical()
  {
    return _nodedatanumerical;
  }
  
  public Timestamp getNodeDataDate()
  {
    return _nodedatadate;
  }
  
  /** Setter methods **/
  
  /**
   * A method used to set the node id of the current node
   *
   * @param id  the new value of the id
   */
  public void setNodeId (long id) {
	  _nodeid = id;
  }
  
  /**
   * A method used to set the node parent id of the current node
   *
   * @param parentid  the new value of the parent id
   */
  public void setParentNodeId (long parentid) {
	  _parentnodeid = parentid;
  }
  
  /**
   * A method used to set the node name of the current node
   *
   * @param name  the new value of the node name
   */
  public void setNodeName (String name) {
	  if (name != null) {
		  _nodename = name.trim();
	  } else {
		  _nodename = null;
	  }
  }
  
  /**
   * A method used to set the node prefix of the current node
   *
   * @param prefix  the new value of the node prefix
   */
  public void setNodePrefix (String prefix) {
	  if (prefix != null) {
		  _nodeprefix = prefix.trim(); 
	  } else {
		  _nodeprefix = null;
	  }
  }
  
  /**
   * A method used to set the node index of the current node
   *
   * @param index  the new value of the node index
   */
  public void setNodeIndex (long index) {
	  _nodeindex = index;
  }
  
  /**
   * A method used to set the node type of the current node
   *
   * @param type  the new value of the node type
   */ 
  public void setNodeType (String type) {
	  if (type != null) {
		  _nodetype = type.trim();
	  } else {
		  _nodetype = null;
	  }
  }
  
  /**
   * A method used to set the node data of the current node
   *
   * @param data  the new value of the node data
   */
  public void setNodeData (String data) {
	  if (data != null) {
		  _nodedata = data.trim();
	  } else {
		  _nodedata = null;
	  }
  }
  
  /**
   * A method used to set the numerical node data of the current node
   *
   * @param datanumerical  the new value of the numerical node data
   */     
  public void setNodeDataNumerical (float datanumerical){
    _nodedatanumerical = datanumerical;
  }
  
  public void setNodeDataDate(Timestamp datadate){
	    _nodedatadate = datadate;
	  }
  
  /** Method compare two records */
  public boolean contentEquals(NodeRecord record)
  {
    boolean flag = true;
    logMetacat.info("First nodetype: " + _nodetype);
    logMetacat.info("Second nodetype: " + record.getNodeType());
    logMetacat.info("First nodename: " + _nodename);
    logMetacat.info("Second nodename: " + record.getNodeName());
    logMetacat.info("First nodeprefix: " + _nodeprefix);
    logMetacat.info("Second nodeprefix: " + record.getNodePrefix());
    logMetacat.info("First nodedata: " + _nodedata);
    logMetacat.info("Second nodedata: " + record.getNodeData());
    if ((_nodename == null && record.getNodeName() != null) ||
        (_nodename != null && record.getNodeName() == null) ||
        (_nodename != null && record.getNodeName() != null &&
        !(_nodename).equals(record.getNodeName())))
    {
      //compare nodename
      flag = false;
    }
    else if ((_nodetype == null && record.getNodeType() != null) ||
             (_nodetype != null && record.getNodeType() == null) ||
             (_nodetype != null && record.getNodeType() != null &&
             !(_nodetype).equals(record.getNodeType())))
    {
      // compare node type
      flag = false;
    }
    else if ((_nodeprefix == null && record.getNodePrefix() != null) ||
             (_nodeprefix != null && record.getNodePrefix() == null) ||
             (_nodeprefix != null && record.getNodePrefix() != null &&
             !(_nodeprefix).equals(record.getNodePrefix())))
    {
      // compare node prefix
      flag = false;
    }
    else if ((_nodedata == null && record.getNodeData() != null) ||
             (_nodedata != null && record.getNodeData() == null) ||
             (_nodedata != null && record.getNodeData() != null &&
             !(_nodedata).equals(record.getNodeData())))
    {
      // compare node data
      flag = false;
    }
    return flag;
    
  }//contentEquals
}
