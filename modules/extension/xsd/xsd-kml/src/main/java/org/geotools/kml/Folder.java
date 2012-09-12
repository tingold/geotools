package org.geotools.kml;

/**
 * Container for folder name hierarchies
 *
 */
public class Folder {

    private String name = null;

    /**
     * Return the folder's name.
     *
     * @return folder's name. Can be null.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the folder's name
     *
     * @param name folder's name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Folder (name=" + name + ")";
    }
}
