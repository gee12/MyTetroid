/*--

 Copyright (C) 2000-2007 Jason Hunter & Brett McLaughlin.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions, and the disclaimer that follows
    these conditions in the documentation and/or other materials
    provided with the distribution.

 3. The name "JDOM" must not be used to endorse or promote products
    derived from this software without prior written permission.  For
    written permission, please contact <request_AT_jdom_DOT_org>.

 4. Products derived from this software may not be called "JDOM", nor
    may "JDOM" appear in their name, without prior written permission
    from the JDOM Project Management <request_AT_jdom_DOT_org>.

 In addition, we request (but do not require) that you include in the
 end-user documentation provided with the redistribution and/or in the
 software itself an acknowledgement equivalent to the following:
     "This product includes software developed by the
      JDOM Project (http://www.jdom.org/)."
 Alternatively, the acknowledgment may be graphical using the logos
 available at http://www.jdom.org/images/logos.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE JDOM AUTHORS OR THE PROJECT
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 This software consists of voluntary contributions made by many
 individuals on behalf of the JDOM Project and was originally
 created by Jason Hunter <jhunter_AT_jdom_DOT_org> and
 Brett McLaughlin <brett_AT_jdom_DOT_org>.  For more information
 on the JDOM Project, please see <http://www.jdom.org/>.

 */

package com.gee12.mytetroid.data.xml;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.output.support.Walker;
import org.jdom2.util.NamespaceStack;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * XMLOutputProcessor without whitespaces before closing tags "/>".
 */
public class TetroidXMLProcessor extends AbstractXMLOutputProcessor {

    /**
     * This will handle printing of an {@link Element}.
     * <p>
     * This method arranges for outputting the Element infrastructure including
     * Namespace Declarations and Attributes.
     *
     * @param out
     *        <code>Writer</code> to use.
     * @param fstack
     *        the FormatStack
     * @param nstack
     *        the NamespaceStack
     * @param element
     *        <code>Element</code> to write.
     * @throws IOException
     *         if the destination Writer fails
     */
    @Override
    protected void printElement(final Writer out, final FormatStack fstack,
                                final NamespaceStack nstack, final Element element) throws IOException {

        nstack.push(element);
        try {
            final List<Content> content = element.getContent();

            // Print the beginning of the tag plus attributes and any
            // necessary namespace declarations
            write(out, "<");

            write(out, element.getQualifiedName());

            // Print the element's namespace, if appropriate
            for (final Namespace ns : nstack.addedForward()) {
                printNamespace(out, fstack, ns);
            }

            // Print out attributes
            if (element.hasAttributes()) {
                for (final Attribute attribute : element.getAttributes()) {
                    printAttribute(out, fstack, attribute);
                }
            }

            if (content.isEmpty()) {
                // Case content is empty
                if (fstack.isExpandEmptyElements()) {
                    write(out, "></");
                    write(out, element.getQualifiedName());
                    write(out, ">");
                }
                else {
                    write(out, "/>");
                }
                // nothing more to do.
                return;
            }

            // OK, we have real content to push.
            fstack.push();
            try {

                // Check for xml:space and adjust format settings
                final String space = element.getAttributeValue("space",
                        Namespace.XML_NAMESPACE);

                if ("default".equals(space)) {
                    fstack.setTextMode(fstack.getDefaultMode());
                }
                else if ("preserve".equals(space)) {
                    fstack.setTextMode(Format.TextMode.PRESERVE);
                }

                // note we ensure the FStack is right before creating the walker
                Walker walker = buildWalker(fstack, content, true);

                if (!walker.hasNext()) {
                    // the walker has formatted out whatever content we had
                    if (fstack.isExpandEmptyElements()) {
                        write(out, "></");
                        write(out, element.getQualifiedName());
                        write(out, ">");
                    }
                    else {
                        write(out, "/>");
                    }
                    // nothing more to do.
                    return;
                }
                // we have some content.
                write(out, ">");
                if (!walker.isAllText()) {
                    // we need to newline/indent
                    textRaw(out, fstack.getPadBetween());
                }

                printContent(out, fstack, nstack, walker);

                if (!walker.isAllText()) {
                    // we need to newline/indent
                    textRaw(out, fstack.getPadLast());
                }
                write(out, "</");
                write(out, element.getQualifiedName());
                write(out, ">");

            } finally {
                fstack.pop();
            }
        } finally {
            nstack.pop();
        }

    }
}
