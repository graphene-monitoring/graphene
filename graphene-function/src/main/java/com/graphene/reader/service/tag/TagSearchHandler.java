package com.graphene.reader.service.tag;

import java.util.List;

public interface TagSearchHandler {
  List<String> getTags(String tagInput, List<String> tagExpressions, Long from, Long to);

  List<String> getTagValues(String tagValueInput, List<String> tagExpressions, String tag, Long from, Long to);
}
