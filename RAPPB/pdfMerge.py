from pypdf import PdfWriter
 
merger = PdfWriter()
 
for pdf in ["/home/g/Projects/LDPC-Decoder/RAPPB/build/throughput/throughput_BG1_vs_0nread_32.pdf", "/home/g/Projects/LDPC-Decoder/RAPPB/build/throughput/throughput_BG1_vs_0nread_256.pdf"]:
    merger.append(pdf)
 
merger.write("merged-pdf.pdf")
merger.close()