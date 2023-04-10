package civil.utils

import civil.models.{CommentNode, CommentReply, EntryWithDepth, TribunalCommentNode, TribunalCommentsReply, TribunalEntryWithDepth}

import java.util.UUID
import scala.annotation.tailrec

object CommentsTreeConstructor {

  def construct[A](
      nodes: List[EntryWithDepth],
      rawNode: List[CommentReply]
  ): List[CommentNode] = {
    val nodesByParent = rawNode.groupBy(_.parentId)
    // val topNodes = nodesByParent.getOrElse(None, Seq.empty)
    val maxDepth = nodes.headOption.map(_.depth).getOrElse(0)
    buildFromBottom(maxDepth, nodes, nodesByParent, Map.empty)
  }

  def constructTribunal[A](
      nodes: List[TribunalEntryWithDepth],
      rawNode: List[TribunalCommentsReply]
  ): List[TribunalCommentNode] = {
    val nodesByParent = rawNode.groupBy(_.parentId)
    // val topNodes = nodesByParent.getOrElse(None, Seq.empty)
    val maxDepth = nodes.headOption.map(_.depth).getOrElse(0)
    buildFromBottomTribunal(maxDepth, nodes, nodesByParent, Map.empty)
  }

  @tailrec
  private def buildFromBottom(
      depth: Int,
      remaining: List[EntryWithDepth],
      nodesByParent: Map[Option[UUID], List[CommentReply]],
      processedNodesById: Map[UUID, CommentNode]
  ): List[CommentNode] = {
    val (nodesOnCurrentDepth, rest) = remaining.span(_.depth == depth)
    val newProcessedNodes = nodesOnCurrentDepth.map { n =>
      val nodeId = n.comment.id
      val children = nodesByParent
        .getOrElse(Some(nodeId), List.empty)
        .flatMap(c => processedNodesById.get(c.id))
      nodeId -> CommentNode(n.comment, children)
    }.toMap
    if (depth > 0) {
      buildFromBottom(
        depth - 1,
        rest,
        nodesByParent,
        processedNodesById ++ newProcessedNodes
      )
    } else {
      // top nodes
      newProcessedNodes.values.toList
    }
  }

  @tailrec
  private def buildFromBottomTribunal(
      depth: Int,
      remaining: List[TribunalEntryWithDepth],
      nodesByParent: Map[Option[UUID], List[TribunalCommentsReply]],
      processedNodesById: Map[UUID, TribunalCommentNode]
  ): List[TribunalCommentNode] = {
    val (nodesOnCurrentDepth, rest) = remaining.span(_.depth == depth)
    val newProcessedNodes = nodesOnCurrentDepth.map { n =>
      val nodeId = n.comment.id
      val children = nodesByParent
        .getOrElse(Some(nodeId), List.empty)
        .flatMap(c => processedNodesById.get(c.id))
      nodeId -> TribunalCommentNode(n.comment, children)
    }.toMap
    if (depth > 0) {
      buildFromBottomTribunal(
        depth - 1,
        rest,
        nodesByParent,
        processedNodesById ++ newProcessedNodes
      )
    } else {
      // top nodes
      newProcessedNodes.values.toList
    }
  }

}
