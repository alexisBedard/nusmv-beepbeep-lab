/*
  A benchmark for NuSMV extensions to BeepBeep 3
  Copyright (C) 2021 Alexis Bédard and Sylvain Hallé

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package nusmvlab;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import ca.uqac.lif.labpal.server.BlankPageCallback;
import ca.uqac.lif.labpal.server.LabPalServer;

/**
 * Page added to the lab's web interface that displays the NuSMV input file
 * generated for a specific experiment.
 */
public class ModelPageCallback extends BlankPageCallback
{
	/**
	 * Creates a new instance of the callback.
	 * @param lab The lab where this callback will be added
	 */
	public ModelPageCallback(LabPalServer server)
	{
		super(server, Method.GET, "/view-model");
		setTitle("NuSMV model");
	}
	
	@Override
	public String getCustomContent(Map<String, String> params)
	{ 
		StringBuilder contents = new StringBuilder();
		if (!params.containsKey("id"))
		{
			contents.append("<p>No experiment ID passed to page.</p>\n");
		}
		else
		{
			int exp_id = Integer.parseInt(params.get("id").trim());
			NuSMVExperiment exp = (NuSMVExperiment) getServer().getLaboratory().getExperiment(exp_id);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			try
			{
				exp.printModel(ps);
				contents.append("<p><a href=\"download-model?id=").append(exp_id).append("\">Download model</a></p>\n\n");
				contents.append("<pre>").append(highlightSMV(baos.toString())).append("</pre>\n");
			}
			catch (IOException e)
			{
				contents.append("<p>The model cannot be printed.</p>\n");
			}
		}
		return contents.toString();
	}
	
	protected static String highlightSMV(String s)
	{
		s = s.replaceAll("&", "&amp;");
		s = s.replaceAll("<", "&lt;");
		s = s.replaceAll(">", "&gt;");
		s = s.replaceAll("MODULE", "<b>MODULE</b>");
		s = s.replaceAll("next", "<b>next</b>");
		s = s.replaceAll("init", "<b>init</b>");
		s = s.replaceAll("case", "<b>case</b>");
		s = s.replaceAll("esac", "<b>esac</b>");
		s = s.replaceAll("ASSIGN", "<b>ASSIGN</b>");
		s = s.replaceAll("VAR", "<b>VAR</b>");
		s = s.replaceAll("CTLSPEC", "<b>CTLSPEC</b>");
		s = s.replaceAll("LTLSPEC", "<b>LTLSPEC</b>");
		return s;
	}

}
