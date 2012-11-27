package org.geotools.data.mongodb;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.Geometries;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeoJSONGeometryBuilder {

    GeometryFactory geomFactory;

    public GeoJSONGeometryBuilder() {
        this(new GeometryFactory());
    }

    public GeoJSONGeometryBuilder(GeometryFactory geomFactory) {
        this.geomFactory = geomFactory;
    }

    public Geometry build(DBObject obj) {
        if (obj == null) {
            return null;
        }
        String type = (String)obj.get("type");
        
        Geometries g = Geometries.getForName(type);
        if (g == null) {
            throw new IllegalArgumentException("Unable to create geometry of type: " + type);
        }

        BasicDBList list = (BasicDBList) obj.get("coordinates");
        switch(g) {
            case POINT:
                return buildPoint(list);
            case LINESTRING:
                return buildLineString(list);
            case POLYGON:
                return buildPolygon(list);
            case MULTIPOINT:
                return buildMultiPoint(list);
            case MULTILINESTRING:
                return buildMultiLineString(list);
            case MULTIPOLYGON:
                return buildMultiPolygon(list);
            case GEOMETRYCOLLECTION:
                return buildGeometryCollection((BasicDBList) obj.get("geometries"));
            default:
                throw new IllegalArgumentException("Unknown geometry type: " + type);
        }
    }

    public GeometryCollection buildGeometryCollection(BasicDBList obj) {
        List<Geometry> geoms = new ArrayList();
        for (Object o : obj) {
            geoms.add(build(obj));
        }
        return geomFactory.createGeometryCollection(geoms.toArray(new Geometry[geoms.size()]));
    }

    public Geometry buildMultiPolygon(List list) {
        List<Polygon> polys = new ArrayList();
        for (Object o : list) {
            polys.add(buildPolygon((List)o));
        }
        return geomFactory.createMultiPolygon(polys.toArray(new Polygon[polys.size()]));
    }

    public Geometry buildMultiLineString(List list) {
        List<LineString> lines = new ArrayList();
        for (Object o : list) {
            lines.add(buildLineString((List)o));
        }
        return geomFactory.createMultiLineString(lines.toArray(new LineString[lines.size()]));
    }

    public MultiPoint buildMultiPoint(List list) {
        List<Point> points = new ArrayList();
        for (Object o : list) {
            points.add(buildPoint((List)o));
        }
        return geomFactory.createMultiPoint(points.toArray(new Point[points.size()]));
    }

    public Polygon buildPolygon(List list) {
        LinearRing outer = (LinearRing) buildLineString((List)list.get(0));
        List<LinearRing> inner = new ArrayList();
        for (int i = 1; i < list.size(); i++) {
            inner.add((LinearRing) buildLineString((List)list.get(i)));
        }
        return geomFactory.createPolygon(outer, inner.toArray(new LinearRing[inner.size()]));
    }

    public LineString buildLineString(List list) {
        List<Coordinate> coordList = new ArrayList<Coordinate>(list.size());
        for (Object o : list) {
            coordList.add(buildCoordinate((List)o));
        }

        Coordinate[] coords = coordList.toArray(new Coordinate[coordList.size()]);
        if (coords.length > 3 && coords[0].equals(coords[coords.length-1])) {
            return geomFactory.createLinearRing(coords);
        }
        return geomFactory.createLineString(coords);
    }

    public Point buildPoint(List list) {
        return geomFactory.createPoint(buildCoordinate(list));
    }

    public Coordinate buildCoordinate(List list) {
        double x = ((Number)list.get(0)).doubleValue();
        double y = ((Number)list.get(1)).doubleValue();
        return new Coordinate(x, y);
    }
}
