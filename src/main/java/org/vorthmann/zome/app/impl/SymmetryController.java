
//(c) Copyright 2007, Scott Vorthmann.  All rights reserved.

package org.vorthmann.zome.app.impl;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.vorthmann.ui.Controller;
import org.vorthmann.ui.DefaultController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.editor.SymmetrySystem;
import com.vzome.core.math.DomUtils;
import com.vzome.core.math.symmetry.Axis;
import com.vzome.core.math.symmetry.Direction;
import com.vzome.core.math.symmetry.OrbitSet;
import com.vzome.core.math.symmetry.Symmetry;
import com.vzome.core.render.Color;
import com.vzome.core.render.RenderedModel;
import com.vzome.core.render.RenderedModel.OrbitSource;
import com.vzome.core.render.Shapes;
import com.vzome.desktop.controller.ViewPlatformModel;

public class SymmetryController extends DefaultController implements RenderedModel.OrbitSource
{
    public String getProperty( String string )
    {
        if ( "renderingStyle" .equals( string ) )
        {
            return this .symmetrySystem .getStyle() .getName();
        }
        else if ( string .startsWith( "orbitColor." ) )
        {
            String name = string .substring( "orbitColor." .length() );
            Direction dir = buildOrbits .getDirection( name );
            Color color = getColor( dir );
            return color .toString();
        }
        return super.getProperty( string );
    }
    
    private SymmetrySystem symmetrySystem;
    public OrbitSet availableOrbits;
    public OrbitSet snapOrbits;
    public OrbitSet buildOrbits;
    public OrbitSet renderOrbits;
    private final ViewPlatformModel.Snapper snapper;
    
    public OrbitSetController availableController;
    public OrbitSetController snapController;
    public OrbitSetController buildController;
    public OrbitSetController renderController;
    
    public Map orbitLengths = new HashMap();
        
    public Symmetry getSymmetry()
    {
        return this .symmetrySystem .getSymmetry();
    }
    
    public ViewPlatformModel.Snapper getSnapper()
    {
        return snapper;
    }
    
    public SymmetryController( Controller parent, SymmetrySystem model )
    {
        this .setNextController( parent );
        this.symmetrySystem = model;
        Symmetry symmetry = model .getSymmetry();
        availableOrbits = new OrbitSet( symmetry );
        snapOrbits = new OrbitSet( symmetry );
        buildOrbits = new OrbitSet( symmetry );
        renderOrbits = new OrbitSet( symmetry );
        snapper = new SymmetrySnapper( snapOrbits );
        boolean haveLoneBuildOrbit = false;
        for ( Iterator dirs = symmetry .getOrbitSet() .iterator(); dirs .hasNext(); )
        {
            Direction dir = (Direction) dirs .next();
            if ( dir .isStandard() )
            {
                availableOrbits .add( dir );
                snapOrbits .add( dir );
                if ( ! haveLoneBuildOrbit )
                {
                    buildOrbits .add( dir );
                    haveLoneBuildOrbit = true;
                }
            }
            renderOrbits .add( dir );
            orbitLengths .put( dir, new LengthController( dir ) );
        }
        availableController = new OrbitSetController( availableOrbits, this .symmetrySystem .getOrbits(), this .symmetrySystem, false );
        availableController .setNextController( this );
        snapController = new OrbitSetController( snapOrbits, availableOrbits, this .symmetrySystem, false );
        snapController .setNextController( this );
        buildController = new OrbitSetController( buildOrbits, availableOrbits, this .symmetrySystem, true );
        buildController .setNextController( this );
        renderController = new OrbitSetController( renderOrbits, this .symmetrySystem .getOrbits(), this .symmetrySystem, false );
        renderController .setNextController( this );

        for ( Iterator dirs = this .symmetrySystem .getOrbits() .iterator(); dirs .hasNext(); )
        {
            Direction dir = (Direction) dirs .next();
            LengthController lengthModel = new LengthController( dir );
            lengthModel .setNextController( buildController );
            orbitLengths .put( dir, lengthModel );
        }
        if ( parent .propertyIsTrue( "disable.known.directions" ) )
        	this .symmetrySystem .disableKnownDirection();

        availableController .addPropertyListener( new PropertyChangeListener()
        {
            public void propertyChange( PropertyChangeEvent event )
            {
                if ( "orbits" .equals( event .getPropertyName() ) )
                {
//                    properties() .firePropertyChange( event ); // just forwarding
                }
            }
        } );
    }
    

    public Controller getSubController( String name )
    {
        if ( name .equals( "availableOrbits" ) )
            return availableController;
        if ( name .equals( "snapOrbits" ) )
            return snapController;
        if ( name .equals( "buildOrbits" ) )
            return buildController;
        if ( name .equals( "renderOrbits" ) )
            return renderController;
        if ( name .startsWith( "length." ) )
        {
            String dirName = name .substring( "length." .length() );
            Direction dir = this .symmetrySystem .getOrbits() .getDirection( dirName );
            return (Controller) orbitLengths .get( dir );
        }
        return null;
    }
    
    public String[] getCommandList( String listName )
    {
        if ( "styles" .equals( listName ) )
        {
            return this .symmetrySystem .getStyleNames();
        }
        if ( listName .equals( "orbits" ) )
        {
            String[] result = new String[ this .symmetrySystem .getOrbits() .size() ];
            int i = 0;
            for ( Iterator orbits = this .symmetrySystem .getOrbits() .iterator(); orbits .hasNext(); i++ )
                result[ i ] = ((Direction) orbits .next()) .getName();
            return result;
        }
        if ( "tool.templates" .equals( listName ) )
        {
            return new String[]{ "icosahedral", "octahedral", "tetrahedral", "rotation", "mirror" };
        }
        return new String[0];
    }

    public void doAction( String action, ActionEvent e ) throws Exception
    {
        if ( action .equals( "rZomeOrbits" )
           || action .equals( "predefinedOrbits" )
           || action .equals( "setAllDirections" ) )
        {
            availableController .doAction( action, e );
        }
        else if ( action .startsWith( "setStyle." ) )
        {
            String styleName =  action .substring( "setStyle." .length() );
            this .symmetrySystem .setStyle( styleName );
            super .doAction( action, e ); // falling through so that rendering gets adjusted
        }
    }

    public Element getXml( Document doc )
    {
        Element symmXml = this .symmetrySystem .getXml( doc );
        // now layer on our controller state, as attributes and subelements
        DomUtils .addAttribute( symmXml, "single", buildController .getProperty( "oneAtATime" ) );
        NodeList nodes = symmXml .getChildNodes();
        for ( int i = 0; i < nodes .getLength(); i++ ) {
            Node node = nodes .item( i );
            if ( node instanceof Element ) {
                Element dirElem = (Element) node;
                String name = dirElem .getAttribute( "name" );
                Direction dir = this .symmetrySystem .getOrbits() .getDirection( name );
                if ( availableOrbits .contains( dir ) )
                    DomUtils .addAttribute( dirElem, "available", "true" );
                if ( snapOrbits .contains( dir ) )
                    DomUtils .addAttribute( dirElem, "snap", "true" );
                if ( buildOrbits .contains( dir ) )
                    DomUtils .addAttribute( dirElem, "build", "true" );
                if ( renderOrbits .contains( dir ) )
                    DomUtils .addAttribute( dirElem, "render", "true" );
                {
                    LengthController lengthModel = (LengthController) orbitLengths .get( dir );
                    if ( lengthModel != null )
                    {
                        Element lengthElem = doc .createElement( "LengthModel" );
                        lengthModel .getXml( lengthElem );
                        dirElem .appendChild( lengthElem );
                    }
                }
            }
        }
        return symmXml;
    }

    public void setXml( Element symmXml )
    {
        // The symmetrySystem has already been loaded from the same XML element,
        //   so now we just need to set our controller state from the additional
        //   attributes and subelements.

        String oneAtATime = symmXml .getAttribute( "single" );
        if ( oneAtATime == null || oneAtATime .isEmpty() )
            oneAtATime = "true";
        buildController .setProperty( "oneAtATime", oneAtATime );
        
        // make sure that the orbit sets have no leftovers from the constructor
        availableOrbits .clear();
        buildOrbits .clear();
        snapOrbits .clear();
        renderOrbits .clear();
        
        NodeList nodes = symmXml .getChildNodes();
        for ( int i = 0; i < nodes .getLength(); i++ ) {
            Node node = nodes .item( i );
            if ( node instanceof Element ) {
                Element dirElem = (Element) node;
                String name = dirElem .getAttribute( "name" );
                Direction dir = this .symmetrySystem .getOrbits() .getDirection( name );
                
                if ( dir == null )
                    continue;  // an old, "unnamed_13"-style direction, with no prototype vector and thus no value

                if ( "true" .equals( dirElem .getAttribute( "available" ) ) )
                    availableController .toggleOrbit( dir );
                if ( "true" .equals( dirElem .getAttribute( "snap" ) ) )
                    snapController .toggleOrbit( dir );
                if ( "true" .equals( dirElem .getAttribute( "build" ) ) )
                    buildController .toggleOrbit( dir );
                if ( "true" .equals( dirElem .getAttribute( "render" ) ) )
                    renderController .toggleOrbit( dir );

                Element length = DomUtils .getFirstChildElement( dirElem, "LengthModel" );
                if ( length != null )
                {
                    LengthController lm = (LengthController) orbitLengths .get( dir );
                    if ( lm == null )
                    {
                        lm = new LengthController( dir );
                        lm .setNextController( buildController );
                        orbitLengths .put( dir, lm );
                    }
                    lm .setXml( length );
                }
            }
        }
    }

    public OrbitSet getOrbits()
    {
        return this .symmetrySystem .getOrbits();
    }
    
    public OrbitSource getOrbitSource()
    {
        return this .symmetrySystem;
    }
    
    // TODO this should take over all functions of symmetry.getAxis()
    
    public Axis getZone( AlgebraicVector offset )
    {
        return getAxis( offset );
    }
    
    public Axis getAxis( AlgebraicVector vector )
    {
    	Axis axis = this .symmetrySystem .getAxis( vector );
    	    	
        boolean allowNonstandard = userHasEntitlement( "all.tools" );  // modeler (not zomepad)
        if ( allowNonstandard ) {
            Direction dir = axis .getDirection();
            if ( ! renderOrbits .contains( dir ) )
            {
                renderOrbits .add( dir );
                LengthController lm = new LengthController( dir );
                orbitLengths .put( dir, lm );
                lm .setNextController( buildController );
            }
        }                
        
        return axis;
    }

    public Color getColor( Direction orbit )
    {
        return this .symmetrySystem .getColor( orbit );
    }

	public OrbitSet getBuildOrbits()
	{
		return this .buildOrbits;
	}

    public Shapes getRenderingStyle()
    {
        return this .symmetrySystem .getRenderingStyle();
    }
}