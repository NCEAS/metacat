The names of the subdirectories contained within this "skins" directory correspond directly with the values of the "qformat" parameter sent as part of the user request. The system will automatically look in the correct folder under style/skins/ based on this value. In there, it expects to find the following (substitute the real value for {qformat}):

subdirectory is style/skins/{qformat}

it contains:

{qformat}.js 
- contains url settings for the globally-included header/left sidebar/right sidebar/footer, where applicable


{qformat}.css 
- contains the css styles for customizing the presentation of the HTML sent back to the client. Also contains the size settings for the globally-included header/left sidebar/right sidebar/footer, where applicable.


{qformat}.xml 
- contains the mappings from doctype-> xsl stylesheet, so the system knows which stylesheet to use for styling any particular requested document.

- any other content referenced in any of these files (for example, it may contain the html for the header, footer etc, and their associated images, if these are not pulled from a separate site)