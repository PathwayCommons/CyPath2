package org.cytoscape.cpathsquared.internal;


final class NvpListItem implements Comparable<NvpListItem> {
    private String name;
    private String value;


    public NvpListItem (String name, String value) {
        this.name = name;
        this.value = value;
    }


    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return (name != null) ? name : value;
    }

	@Override
	public int compareTo(NvpListItem o) {
		return (name != null) 
			? this.name.compareTo(o.getName())
			: this.value.compareTo(o.getValue());
	}
}
