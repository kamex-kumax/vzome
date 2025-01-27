
package com.vzome.core.viewing;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.Polyhedron;
import com.vzome.core.math.RealVector;
import com.vzome.core.parts.StrutGeometry;

public class ExportedVEFStrutGeometry implements StrutGeometry
{
    @JsonProperty( "vertices" )
    public final List<AlgebraicVector> prototypeVertices;  // the polyhedron from which others are derived

    @JsonProperty( "polygons" )
    public final List< List<Integer> > prototypeFaces;

    private final AlgebraicField field;

    public final AlgebraicVector prototypeVector; // the prototype strut vector from the symmetry group

    public final Set<Integer> fullScaleVertices, halfScaleVertices;  // the polyhedron vertices that must adjust for different strut lengths

    public ExportedVEFStrutGeometry( List<AlgebraicVector> vertices, List< List<Integer> > faces, AlgebraicVector prototype, Set<Integer> fullScaleVertices, Set<Integer> halfScaleVertices, AlgebraicField field )
    {
        prototypeVertices = vertices;
        prototypeFaces = faces;
        prototypeVector = prototype;
        this.fullScaleVertices = fullScaleVertices;
        this.halfScaleVertices = halfScaleVertices;
        this.field = field;
    }

    public ExportedVEFStrutGeometry( List<AlgebraicVector> vertices, List< List<Integer> > faces, AlgebraicVector prototype, Set<Integer> fullScaleVertices, AlgebraicField field )
    {
        this( vertices, faces, prototype, fullScaleVertices, null, field );
    }

    @Override
    public Polyhedron getStrutPolyhedron( AlgebraicNumber length )
    {
        AlgebraicVector tipVertex = prototypeVector .scale( length );
        AlgebraicVector midpoint = tipVertex .scale( this .field .createRational( 1, 2 ) );
        if ( field .getName() .equals( "snubDodec" ) )
        {
            RealVector rproto = prototypeVector .toRealVector();
            System .out .println( "proto length = " + rproto .length() );
            double lend = length .evaluate();
            System .out .println( "strut length = " + lend );
            RealVector rtip = tipVertex .toRealVector();
            System .out .println( "tip length = " + rtip .length() );
        }
        Polyhedron result = new Polyhedron( field );
        for ( int i = 0; i < prototypeVertices .size(); i ++ )
        {
            AlgebraicVector vertex = prototypeVertices .get( i );
            if ( fullScaleVertices .contains(i) ) {
                vertex = vertex .plus( tipVertex );
            } else if ( halfScaleVertices != null && halfScaleVertices .contains(i) ) {
                vertex = vertex .plus( midpoint );
            }
            result .addVertex( vertex );
        }
        for (List<Integer> prototypeFace : prototypeFaces) {
            Polyhedron.Face face = result .newFace();
            face .addAll( prototypeFace );
            result .addFace( face );
        }
        return result;
    }
}
