library(sf)
library(tidyverse)

# This file contains code to download and extract street network data from AGRC,
# and transit data for UTA.


# Get AGRC Network data ==========
# The Utah AGRC multimodal network database is available at 
#   https://gis.utah.gov/data/transportation/street-network-analysis/#MultimodalNetwork
#   
# For this research we downloaded the information from that file in August 2020.
# The file we downloaded is available on Box, but is not committed to the 
# repository for space reasons. This file contains code to download the archived 
# AGRC network, extract it. 
filegdb <- "data/MM_NetworkDataset_07272020.gdb"
if(!file.exists(filegdb)) {
  zippedgdb <- "data/agrc_network.zip"
  if(!file.exists(zippedgdb)) {
    download.file("https://byu.box.com/shared/static/qyjf1of9dau7tyc3rptxc9w4fi08x3d6.zip",
      zippedgdb)
  }
  system2("7z", c("e", zippedgdb, str_c("-o", filegdb)) )
} 
  

#

# Pull network data from GDB ===============



