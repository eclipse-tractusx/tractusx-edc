package net.catenax.edc.tests.api.datamanagement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class StatusCodeResponseHandler implements ResponseHandler<Void> {
  @NonNull private final List<Integer> acceptableStatusCodes;

  StatusCodeResponseHandler(@NonNull final Integer... codes) {
    this.acceptableStatusCodes = Arrays.asList(codes);
  }

  @Override
  public Void handleResponse(final HttpResponse response) throws IOException {
    final StatusLine statusLine = response.getStatusLine();
    final HttpEntity entity = response.getEntity();
    EntityUtils.consume(entity);
    if (!acceptableStatusCodes.contains(statusLine.getStatusCode())) {
      throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
    }
    return null;
  }
}
