package edu.ucsb.nceas.metacat.index.parser;

import edu.ucsb.nceas.metacat.index.parser.utility.FilterRootElement;
import org.dataone.cn.indexer.parser.SolrField;
import org.dataone.cn.indexer.parser.utility.LogicalOrPostProcessor;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.util.ArrayList;
import java.util.List;

/**
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/**
 * A complex data value mining SolrField. This class returns a value to
 * an indexing subprocessor from its dependent class. See FilterRootElement for
 * a typical usage.
 *
 * @author slaughter
 *
 * Based on CommonRootSolrField by sroseboo
 *
 */
public class FilterCommonRootSolrField extends SolrField {

    private FilterRootElement root;

    private LogicalOrPostProcessor orProcessor = new LogicalOrPostProcessor();

    public FilterCommonRootSolrField(String name) {
        this.name = name;
    }

    @Override
    public List<SolrElementField> getFields(Document doc, String identifier) throws Exception {

        List<SolrElementField> fields = new ArrayList<SolrElementField>();
        String resultValue = null;
        if (root != null) {
            resultValue = root.getRootValues(doc);
        }

        fields.add(new SolrElementField("memberQuery", resultValue));
        return fields;
    }

    @Override
    public void initExpression(XPath xpathObject) {
        root.initXPathExpressions(xpathObject);
    }

    public FilterRootElement getRoot() {
        return root;
    }

    public void setRoot(FilterRootElement root) {
        this.root = root;
    }
}

