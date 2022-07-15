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

import java.util.Map;

import ca.uqac.lif.labpal.server.BlankPageCallback;
import ca.uqac.lif.labpal.server.LabPalServer;

/**
 * Page added to the lab's web interface that displays all the processor
 * pipelines that are included in the experiments.
 */
public class AllQueriesCallback extends BlankPageCallback
{
	/**
	 * Creates a new instance of the callback.
	 * @param lab The lab this callback is associated to
	 */
	public AllQueriesCallback(LabPalServer server)
	{
		super(server, Method.GET, "/queries");
		setTitle("Pipelines");
	}
	
	@Override
	public String getCustomContent(Map<String, String> params) 
	{
		StringBuilder contents = new StringBuilder();
		contents.append("<p>Here is a graphical representation of all the pipelines included ");
		contents.append("in this lab.</p>\n\n");
		for (String query : NuSMVModelLibrary.getQueryNames())
		{
			contents.append("<h3>").append(query).append("</h3>\n");
			String image_url = NuSMVModelLibrary.getImageUrl(query);
			if (image_url != null)
			{
				contents.append("<img src=\"").append(image_url).append("\" alt=\"Processor pipeline\" />\n\n");
			}
		}
		return contents.toString();
	}
}
