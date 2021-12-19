package org.pathwaycommons.cypath2.internal;


/**
 * Model Object for Encapsulating an External Link.
 *
 * @author Ethan Cerami
 */
public class ExternalLink {
  private String dbName;
  private String id;
  private String relType;
  private String title;
  private String year;
  private String author;
  private String url;
  private String source;

  /**
   * Constructor.
   *
   * @param dbName Database name.
   * @param id     Unique ID.
   */
  public ExternalLink(String dbName, String id) {
    this.dbName = dbName;
    this.id = id;
  }

  /**
   * Gets the Database Name.
   *
   * @return Database Name.
   */
  public String getDbName() {
    return dbName;
  }

  /**
   * Sets the Database Name.
   *
   * @param dbName Database Name.
   */
  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  /**
   * Gets the Unique ID.
   *
   * @return Unique ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the Unique ID.
   *
   * @param id Unique ID.
   */
  public void setId(String id) {
    this.id = id;
  }

  public String getRelType() {
    return relType;
  }

  public void setRelType(String relType) {
    this.relType = relType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
