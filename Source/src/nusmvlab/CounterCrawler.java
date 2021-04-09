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

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.PipeCrawler;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.tmf.Window;

/**
 * Crawler that counts the processors encountered inside a chain. The crawler
 * can be made recursive and visit pipelines inside other processors, such as
 * {@link Window} or {@link GroupProcessor}. 
 */
public class CounterCrawler extends PipeCrawler
{
	/**
	 * The total number of processors encountered
	 */
	protected int m_total;
	
	/**
	 * Determines if the crawler enters processors that take other processors
	 * as parameters.
	 */
	protected boolean m_recursive;
	
	/**
	 * Recursively counts all processors in a pipeline.
	 * @param start The starting point of the pipeline
	 * @return The total number of processors
	 */
	public static int countAllProcessors(Processor start)
	{
		CounterCrawler crawler = new CounterCrawler(true);
		crawler.crawl(start);
		return crawler.getCount();
	}
	
	/**
	 * Creates a new counter crawler.
	 * @param recursive Set to <tt>true</tt> to make the crawler enter
	 * processors that take other processors as parameters (such as
	 * {@link Window})
	 */
	public CounterCrawler(boolean recursive)
	{
		super();
		m_total = 0;
		m_recursive = recursive;
	}

	@Override
	public void visit(Processor p)
	{
		m_total++;
		if (m_recursive)
		{
			if (p instanceof Window)
			{
				Window w = (Window) p;
				Processor in = w.getProcessor();
				CounterCrawler in_crawler = new CounterCrawler(m_recursive);
				in_crawler.crawl(in);
				m_total += in_crawler.getCount();
			}
			if (p instanceof GroupProcessor)
			{
				GroupProcessor g = (GroupProcessor) p;
				Processor in = g.getAssociatedInput(0);
				CounterCrawler in_crawler = new CounterCrawler(m_recursive);
				in_crawler.crawl(in);
				m_total += in_crawler.getCount();
			}
		}
	}
	
	/**
	 * Gets the total number of processors encountered by this crawler.
	 * @return The number of processors
	 */
	public int getCount()
	{
		return m_total;
	}
}
