package com.pdf.server;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pdf.ReplaceStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

public class HttpPdfServerHandler extends SimpleChannelInboundHandler<Object> {

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();
	public static final String DESTFILENAME = "filename";
	public static final String PDFFILE = ".pdf";

	private ReplaceStream stream = new ReplaceStream();

	private static Logger logger = LoggerFactory.getLogger(HttpPdfServerHandler.class);

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws IOException {
		if (msg instanceof HttpRequest) {
			HttpRequest request = this.request = (HttpRequest) msg;
			logger.debug("Request "+request.uri());
			if (!request.uri().contains(DESTFILENAME) && request.uri().contains(PDFFILE)) {
				requestHandleStreamPdf(ctx, msg);
			}

			if (request.uri().contains(DESTFILENAME)) {
				requestHandleCreatePdf(ctx, msg);
			}
		}

	}

	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

	private String sanitizeUri(String uri) throws IOException {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		if (!uri.startsWith("/")) {
			return null;
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()) {
			return null;
		}

		// Convert to absolute path.
		return stream.getConfiguration().getDestinationFile(uri);
	}

	private void writeResponse(ChannelHandlerContext ctx) {
		// Decide whether to close the connection or not.
		// Build the response object.
		ByteBuf content = Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8);
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
		// Write the response.
		ctx.write(response);
	}

	private void requestHandleStreamPdf(ChannelHandlerContext ctx, Object msg) throws IOException {
		String outputFileAddress = sanitizeUri(request.uri());
		writePDFResponse(ctx, outputFileAddress);
	}

	private void requestHandleCreatePdf(ChannelHandlerContext ctx, Object msg) {
		try {
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
			Map<String, List<String>> params = queryStringDecoder.parameters();
			String dest = params.get(DESTFILENAME).get(0);
			params.remove(DESTFILENAME);
			stream.manipulatePdf(dest, params);
			String sucess = "%s file created successfully";
			String string = String.format(sucess, dest);
			ByteBuf content = Unpooled.copiedBuffer(string.toString(), CharsetUtil.UTF_8);
			FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, string.length());
			// Write the response.
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
			ctx.close();
			logger.debug(sucess);
		} catch (Throwable e) {
			logger.error(String.format("Failed to create pdf due to [%s]", e));
			ctx.close();
		}
		if (msg instanceof HttpContent) {
			if (msg instanceof LastHttpContent) {
				writeResponse(ctx);
			}
		}
	}

	private void writePDFResponse(ChannelHandlerContext ctx, String outputFileAddress) throws IOException {
		java.nio.file.Path path = Paths.get(outputFileAddress);
		byte[] data = Files.readAllBytes(path);
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(data));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/pdf");
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.length);
		response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + outputFileAddress);
		// Write the response.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error(cause.toString());
		cause.printStackTrace();
		ctx.close();
	}
}