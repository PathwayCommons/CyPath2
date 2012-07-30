package org.cytoscape.cpathsquared.internal;

/**
 * Immutable name-value pair (String, String) type
 * to be used in lists. Name is to display and sort list
 * items, whereas value (perhaps unique) is to be used in an action.
 * 
 * @author rodche
 *
 */
final class NvpListItem implements Comparable<NvpListItem> {
    private final String name;
    private final String value;

    /**
     * Constructor.
     * 
     * @param name if null, value will be used as name
     * @param value not null, id
     * @throws IllegalArgumentException when value is null.
     */
    public NvpListItem (String name, String value) {
    	if(value == null)
    		throw new IllegalArgumentException("Value cannot be Null.");
        this.name = (name != null) ? name : value;
        this.value = value;
    }

    public String getName() {
        return new String(name);
    }

    public String getValue() {
        return new String(value);
    }

    @Override
    public String toString() {
        return getName();
    }

	@Override
	public int compareTo(NvpListItem o) {
		return this.toString().compareTo(o.toString());
	}
	
	
	/**
	 * Only 'value' is considered
	 * ('name' is ignored)
	 * 
	 */
	@Override
	public int hashCode() {
		return value.hashCode();
	}
	
	
	/**
	 * Only 'value' is considered
	 * ('name' is ignored)
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof NvpListItem 
			&& value.equals(((NvpListItem)obj).value);
	};
}
