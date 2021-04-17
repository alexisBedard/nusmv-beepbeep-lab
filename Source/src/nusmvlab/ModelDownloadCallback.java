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

import com.sun.net.httpserver.HttpExchange;

import ca.uqac.lif.jerrydog.CallbackResponse;
import ca.uqac.lif.jerrydog.CallbackResponse.ContentType;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.server.WebCallback;

/**
 * Page added to the lab's web interface that sends the NuSMV model to the
 * user as a downloadable file.
 */
public class ModelDownloadCallback extends WebCallback
{
	/**
	 * Creates a new instance of the callback.
	 * @param lab The lab where this callback will be added
	 */
	public ModelDownloadCallback(Laboratory lab)
	{
		super("/download-model", lab, null);
	}

	@Override
	public CallbackResponse process(HttpExchange t)
	{
		CallbackResponse response = new CallbackResponse(t);
    Map<String, String> params = getParameters(t);
    if (!params.containsKey("id"))
    {
    	response.setContents("<p>No experiment ID is provided.</p>");
    	response.setCode(CallbackResponse.HTTP_BAD_REQUEST);
    	return response;
    }
    int exp_id = Integer.parseInt(params.get("id").trim());
		NuSMVExperiment exp = (NuSMVExperiment) m_lab.getExperiment(exp_id);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		try
		{
			exp.printModel(ps);
		}
		catch (IOException e)
		{
			response.setContents("<p>The model cannot be printed.</p>");
    	response.setCode(CallbackResponse.HTTP_BAD_REQUEST);
    	return response;
		}
		response.setAttachment("model.smv");
		response.setContentType(ContentType.TEXT);
		response.setContents(baos.toString());
    return response;
	}
}