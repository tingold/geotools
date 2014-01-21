package org.geotools.data.mongodb;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.Geometries;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
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

    public Geometry toGeometry(DBObject obj) {
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
                return toPoint(list);
            case LINESTRING:
                return toLineString(list);
            case POLYGON:
                return toPolygon(list);
            case MULTIPOINT:
                return toMultiPoint(list);
            case MULTILINESTRING:
                return toMultiLineString(list);
            case MULTIPOLYGON:
                return toMultiPolygon(list);
            case GEOMETRYCOLLECTION:
                return toGeometryCollection((BasicDBList) obj.get("geometries"));
            default:
                throw new IllegalArgumentException("Unknown geometry type: " + type);
        }
    }

    public DBObject toObject(Geometry geom) {
        Geometries g = Geometries.get(geom);
        switch(g) {
            case POINT:
                return toObject((Point)geom);
            case LINESTRING:
                return toObject((LineString)geom);
            case POLYGON:
                return toObject((Polygon)geom);
            case MULTIPOINT:
                return toObject((MultiPoint)geom);
            case MULTILINESTRING:
                return toObject((MultiLineString)geom);
            case MULTIPOLYGON:
                return toObject((MultiPolygon)geom);
            case GEOMETRYCOLLECTION:
                return toObject((GeometryCollection) geom);
            default:
                throw new IllegalArgumentException("Unknown geometry type: " + geom);
        }
    }

    public GeometryCollection toGeometryCollection(BasicDBList obj) {
        List<Geometry> geoms = new ArrayList();
        for (Object o : obj) {
            geoms.add(toGeometry(obj));
        }
        return geomFactory.createGeometryCollection(geoms.toArray(new Geometry[geoms.size()]));
    }

    public DBObject toObject(GeometryCollection gc) {
        return null;
    }

    public MultiPolygon toMultiPolygon(List list) {
        List<Polygon> polys = new ArrayList();
        for (Object o : list) {
            polys.add(toPolygon((List)o));
        }
        return geomFactory.createMultiPolygon(polys.toArray(new Polygon[polys.size()]));
    }

    public DBObject toObject(MultiPolygon mp) {
        List l = new BasicDBList();
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            l.add(toList(((Polygon)mp.getGeometryN(i))));
        }
        return BasicDBObjectBuilder.start()
            .add("type", "MultiPolygon")
            .add("coordinates", l)
            .get();
    }
    
    public MultiLineString toMultiLineString(List list) {
        List<LineString> lines = new ArrayList();
        for (Object o : list) {
            lines.add(toLineString((List)o));
        }
        return geomFactory.createMultiLineString(lines.toArray(new LineString[lines.size()]));
    }

    public DBObject toObject(MultiLineString ml) {
        List l = new BasicDBList();
        for (int i = 0; i < ml.getNumGeometries(); i++) {
            l.add(toList(((LineString)ml.getGeometryN(i)).getCoordinateSequence()));
        }

        return BasicDBObjectBuilder.start()
            .add("type", "MultiLineString")
            .add("coordinates", l)
            .get();
    }

    public MultiPoint toMultiPoint(List list) {
        List<Point> points = new ArrayList();
        for (Object o : list) {
            points.add(toPoint((List)o));
        }
        return geomFactory.createMultiPoint(points.toArray(new Point[points.size()]));
    }

    public DBObject toObject(MultiPoint mp) {
        return BasicDBObjectBuilder.start()
            .add("type", "MultiPoint")
            .add("coordinates", toList(mp.getCoordinates()))
            .get();
    }

    public Polygon toPolygon(List list) {
        LinearRing outer = (LinearRing) toLineString((List)list.get(0));
        List<LinearRing> inner = new ArrayList();
        for (int i = 1; i < list.size(); i++) {
            inner.add((LinearRing) toLineString((List)list.get(i)));
        }
        return geomFactory.createPolygon(outer, inner.toArray(new LinearRing[inner.size()]));
    }

    public DBObject toObject(Polygon p) {
        return BasicDBObjectBuilder.start()
            .add("type", "Polygon")
            .add("coordinates", toList(p))
            .get();
    }

    public LineString toLineString(List list) {
        List<Coordinate> coordList = new ArrayList<Coordinate>(list.size());
        for (Object o : list) {
            coordList.add(toCoordinate((List)o));
        }

        Coordinate[] coords = coordList.toArray(new Coordinate[coordList.size()]);
        if (coords.length > 3 && coords[0].equals(coords[coords.length-1])) {
            return geomFactory.createLinearRing(coords);
        }
        return geomFactory.createLineString(coords);
    }

    public DBObject toObject(LineString l) {
        return BasicDBObjectBuilder.start()
            .add("type", "LineString")
            .add("coordinates", toList(l.getCoordinateSequence()))
            .get();
    }

    public Point toPoint(List list) {
        return geomFactory.createPoint(toCoordinate(list));
    }

    public DBObject toObject(Point p) {
        return BasicDBObjectBuilder.start()
            .add("type", "Point")
            .add("coordinates", toList(p.getCoordinate()))
            .get();
    }

    public Coordinate toCoordinate(List list) {
        double x = ((Number)list.get(0)).doubleValue();
        double y = ((Number)list.get(1)).doubleValue();
        return new Coordinate(x, y);
    }

    List toList(Coordinate c) {
        BasicDBList l = new BasicDBList();
        l.add(c.x);
        l.add(c.y);
        return l;
    }

    List toList(CoordinateSequence cs) {
        BasicDBList l = new BasicDBList();
        for (int i = 0; i < cs.size(); i++) {
            BasicDBList m = new BasicDBList();
            m.add(cs.getX(i));
            m.add(cs.getY(i));
            l.add(m);
        }
        return l;
    }

    List toList(Coordinate[] cs) {
        BasicDBList l = new BasicDBList();
        for (int i = 0; i < cs.length; i++) {
            BasicDBList m = new BasicDBList();
            m.add(cs[i].x);
            m.add(cs[i].y);
            l.add(m);
        }
        return l;
    }

    List toList(Polygon p) {
        BasicDBList l = new BasicDBList();
        l.add(toList(p.getExteriorRing().getCoordinateSequence()));
        for (int i = 0; i < p.getNumInteriorRing(); i++) {
            l.add(toList(p.getInteriorRingN(i).getCoordinateSequence()));
        }
        return l;
    }
}
