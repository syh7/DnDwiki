# DnDwiki

test

## Setup

- Raspberry Pi 3B
- Bookstack through Docker
- DNS anonymization through Cloudflare

## Steps to set up Raspberry Pi with Docker

1. Setup raspberry pi with basic OS
2. Make sure you can connect with raspberry pi through ssh
    - Create SSH key pair
    - Add the public key to `~/.ssh/authorized_keys` on the raspberry pi
    - Add the private key to `{user}/.ssh` on the pc
    - Test the connection by using `ssh {profile name}@{raspberry pi IP address}`
3. Install docker on raspberry pi `curl -sSL https://get.docker.com | sh`

Sources:

- https://www.raspberrypi.com/documentation/computers/remote-access.html#ssh
- https://docs.requarks.io/install/docker

## Steps to set up bookstack on the raspberry pi:

1. Create a directory `bookstack`
2. In that directory create a `docker-compose.yml` :

   ```
   version: "3"
   services:
     bookstack:
       image: lscr.io/linuxserver/bookstack
       container_name: bookstack
       ports: # open <raspberry pi port> : <container port>
         - '80:80'
         - '443:443'
       environment:
         - PUID=1000
         - PGID=1000
         - APP_URL=https://<yourwebsite>
         - DB_HOST=bookstack_db
         - DB_USER=bookstack
         - DB_PASS=<yourdbpass>
         - DB_DATABASE=bookstackapp
       volumes:
         - ./config:/config
       restart: unless-stopped
       depends_on:
         - bookstack_db
         
     bookstack_db:
       image: lscr.io/linuxserver/mariadb
       container_name: bookstack_db
       environment:
         - PUID=1000
         - PGID=1000
         - MYSQL_ROOT_PASSWORD=<yourdbpass>
         - TZ=Europe/Amsterdam
         - MYSQL_DATABASE=bookstackapp
         - MYSQL_USER=bookstack
         - MYSQL_PASSWORD=<yourdbpass>
       volumes:
         - ./database:/config
       restart: unless-stopped
   ```

3. Run the bookstack image `docker compose up`. Check for errors in logging
   In the future, start the images by running `docker compose up -d` to start them detached, so you can close the
   terminal later
4. Change admin login information for bookstack
5. Create an API key, save the token id and token secret

Sources:

- https://docs.linuxserver.io/images/docker-bookstack/

## Setup DNS and port forwarding

1. Register a website with a registrar
2. Create a cloudflare account and set up your website there
3. Make sure that the nameservers from the registrar point to the cloudflare name servers
4. Find out the public ip address of the raspberry pi
5. Point the cloudflare DNS records to your public ip address
6. Open the admin page of your home modem
7. Setup port forwarding for port 80 and 443 to the raspberry pi

## Show embedded PDFs

1. Add the following code to the `Settings > Customization > Custom HTML Head Content`

   ```@html
   <!-- Frank probeert pdf-previews https://github.com/BookStackApp/BookStack/issues/705#issuecomment-1203691993  -->
   <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.0.466/pdf.min.js"></script>
   <style>
   canvas[data-pdfurl] {
     background-color: lightgrey;
      width: 100%;
   }
   .page-content a {
    color: #39f;
   text-decoration: underline;
   }
   .pdf-wrapper {
   position: relative;
   height: 80vh;
   width: 100%;
   }
   .pdf-wrapper .download-link {
   position: absolute;
   top: -2em; 
   right: 0;
   z-index: 50;
   }
   .pdf-wrapper .pdf-scroller {
   height: 100%;
   overflow: auto;
   }
   </style>
   <script type="text/javascript">
   
     // ------------------- THIS SECTION ADDS A PDF BUTTON TO THE EDITOR TOOLBAR THAT ALLOWS YOU TO EMBED PDFS 
   
     // Use BookStack editor event to add custom "Insert PDF" button into main toolbar
     window.addEventListener('editor-tinymce::pre-init', event => {
         const mceConfig = event.detail.config;
         mceConfig.toolbar = mceConfig.toolbar.replace('link', 'link insertpdf')
     });
   
     // Use BookStack editor event to define the custom "Insert PDF" button.
     window.addEventListener('editor-tinymce::setup', event => {
       const editor = event.detail.editor;
   
       // Add PDF insert button
       editor.ui.registry.addButton('insertpdf', {
         tooltip: 'Insert PDF',
         icon: 'document-properties',
         onAction() {
           editor.windowManager.open({
             title: 'Insert PDF',
             body: {
               type: 'panel',
               items: [
                 {type: 'textarea', name: 'pdfurl', label: 'PDF URL'}
               ]
             },
             onSubmit: function(e) {
               // Insert content when the window form is submitted
               editor.insertContent('<p>&nbsp;<canvas data-pdfurl="' + e.getData().pdfurl + '"></canvas>&nbsp;</p>');
               e.close();
             },
             buttons: [
               {
                 type: 'submit',
                 text: 'Insert PDF'
               }
             ]
           });
         }
       });
   
     });
   
   //-------------------- THE CODE BELOW SHALL BE ACTIVE IN VIEWING MODE TO EMBED PDFS
   var renderPdf=function(canvas) {
     var url = canvas.dataset.pdfurl;
     var pdf = null;
     // wrap canvas in div
     var wrapper = document.createElement('div');
     wrapper.className='pdf-wrapper';
     var scroller = document.createElement('div');
     scroller.className='pdf-scroller';
     wrapper.appendChild(scroller);
     canvas.parentNode.insertBefore(wrapper, canvas.nextSibling);
     scroller.insertBefore(canvas, null);
   
     var downloadLink  = document.createElement('a');
     downloadLink.href = url;
     downloadLink.className="download-link";
     downloadLink.innerText = 'Download PDF now ↓';
     wrapper.appendChild(downloadLink);
   
     var renderPage = function(page) {
       var scale = 1.5;
       var viewport = page.getViewport(scale);
       // Fetch canvas' 2d context
       var context = canvas.getContext('2d');
       // Set dimensions to Canvas
       canvas.height = viewport.height;
       canvas.width = viewport.width;
       canvas.style.maxWidth='100%';
       // Prepare object needed by render method
       var renderContext = {
         canvasContext: context,
         viewport: viewport
       };
       // Render PDF page
       page.render(renderContext);
       if (currentPage < pdf.numPages) {
         currentPage++;
         var newCanvas = document.createElement('canvas');
         scroller.insertBefore(newCanvas, canvas.nextSibling);
         scroller.insertBefore(document.createElement('hr'), canvas.nextSibling);
         canvas=newCanvas;
         pdf.getPage(currentPage).then(renderPage);
       }
     };
     var currentPage = 1;
     pdfjsLib.getDocument(url)
     .then(function(pdfLocal) {
       pdf = pdfLocal;
       return pdf.getPage(1);
     })
     .then(renderPage);
   };
   
   
   window.addEventListener('DOMContentLoaded', function() {
     Array.prototype.forEach.call(document.querySelectorAll('canvas[data-pdfurl]'), renderPdf);
   });
   </script>
   ```

2. Save the PDF to bookstack
3. On the page you want to show the PDF, click the `Insert PDF` button in the toolbar, and fill in the URL to the pDF on
   the bookstack server