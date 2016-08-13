package org.vorthmann.zome.app.impl;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import org.vorthmann.ui.DefaultController;

import com.vzome.core.editor.DocumentModel;
import com.vzome.core.editor.Tool;
import com.vzome.core.editor.TransformationTool;

public class ToolController extends DefaultController
{
	private Tool tool;
	private DocumentModel applier;  // TODO this should be a ToolsModel
	private final String kind;
	private final String label;
	private boolean deleteInputs;
	private boolean selectInputs;
	private boolean selectOutputs;
	private boolean justSelect;

	public ToolController( Tool tool, DocumentModel applier )
	{
		this .tool = tool;
		this .applier = applier;

		String idAndName = tool .getName(); // will be "kind.N/label"
		int delim = idAndName .indexOf( "." );
		this .kind = idAndName .substring( 0, delim );
		delim = idAndName .indexOf( "/" );
		this .label = idAndName .substring( delim + 1 );
		switch (kind) {

		case "icosahedral":
			this .selectInputs = true;
			break;

		case "translation":
		case "scaling":
			this .deleteInputs = true;
			break;

		default:
			break;
		}
	}

	@Override
	public void actionPerformed( ActionEvent e )
	{
		switch ( e .getActionCommand() ) {

		case "apply":
			// TODO use the checkbox modes, override with modifiers
			int modes = 0;
			if ( deleteInputs )
				modes &= ActionEvent.CTRL_MASK;
			if ( selectInputs )
				modes &= ActionEvent.SHIFT_MASK;
			if ( !selectOutputs )
				modes &= ActionEvent.ALT_MASK;
			if ( justSelect )
				modes &= ActionEvent.META_MASK;
			this .applier .applyTool( tool, this .applier, modes );
			break;

		case "selectParams":
			this .applier .selectToolParameters( (TransformationTool) tool );
			break;

		case "help":
			break;

		default:
			super .actionPerformed( e );
		}
	}

	@Override
	public String getProperty( String name )
	{
		switch ( name ) {

		case "label":
			return this .label;

		case "kind":
			return this .kind;

		case "selectInputs":
			return Boolean .toString( this .selectInputs );

		case "deleteInputs":
			return Boolean .toString( this .deleteInputs );

		case "selectOutputs":
			return Boolean .toString( this .selectOutputs );

		case "justSelect":
			return Boolean .toString( this .justSelect );

		default:
			return super .getProperty(name);
		}
	}

	@Override
	public boolean[] enableContextualCommands( String[] menu, MouseEvent e )
	{
		boolean[] result = new boolean[ menu .length ];
		for (int i = 0; i < result.length; i++) {
			result[ i ] = true;
		}
		return result;
	}
}
