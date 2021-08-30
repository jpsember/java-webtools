/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package js.webtools;

import static js.base.Tools.*;

import java.util.List;

public abstract class WebRequestManager implements WebRequestExecutor {

  public WebRequestManager() {
  }

  /**
   * Install a filter to redirect requests
   * 
   * For test purposes only
   */
  public final void installFilter(RequestFilter filter) {
    mRequestFilters.add(filter);
  }

  public final WebRequestExecutor getPossiblyFilteredRequestExecutor(WebRequest request) {
    WebRequestExecutor executor = null;
    if (!mRequestFilters.isEmpty()) {
      for (RequestFilter f : mRequestFilters) {
        if (f.matches(request.uri())) {
          executor = f.executor;
          break;
        }
      }
    }
    if (executor == null) {
      if (mUnfilteredDisabled) {
        // If host is wikipedia, it is (presumably) a unit test; so suppress the following warning 
        if (!request.uri().getHost().equals("www.wikipedia.org"))
          pr("Unfiltered request attempt!", INDENT, request, CR, ST);
        throw new IllegalStateException("cannot make unfiltered request");
      }
    }
    return executor;
  }

  public final void setAllowUnfilteredRequests(boolean allowed) {
    testOnlyAssert();
    mUnfilteredDisabled = !allowed;
  }

  public final boolean containsFilters() {
    return !mRequestFilters.isEmpty();
  }

  private List<RequestFilter> mRequestFilters = arrayList();
  private boolean mUnfilteredDisabled;

}
