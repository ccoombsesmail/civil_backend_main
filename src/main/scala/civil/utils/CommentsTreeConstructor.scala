package civil.utils

import civil.models.{CommentNode, CommentReply, EntryWithDepth, TribunalCommentNode, TribunalCommentsReply, TribunalEntryWithDepth}
import civil.models._

import java.util.UUID
import scala.annotation.tailrec

object CommentsTreeConstructor {

  def construct[A](
      nodes: Seq[EntryWithDepth],
      rawNode: Seq[CommentReply]
  ): Seq[CommentNode] = {
    val nodesByParent = rawNode.groupBy(_.parentId)
    // val topNodes = nodesByParent.getOrElse(None, Seq.empty)
    val maxDepth = nodes.headOption.map(_.depth).getOrElse(0)
    buildFromBottom(maxDepth, nodes, nodesByParent, Map.empty)
  }

  def constructTribunal[A](
      nodes: Seq[TribunalEntryWithDepth],
      rawNode: Seq[TribunalCommentsReply]
  ): Seq[TribunalCommentNode] = {
    val nodesByParent = rawNode.groupBy(_.parentId)
    // val topNodes = nodesByParent.getOrElse(None, Seq.empty)
    val maxDepth = nodes.headOption.map(_.depth).getOrElse(0)
    buildFromBottomTribunal(maxDepth, nodes, nodesByParent, Map.empty)
  }

  @tailrec
  private def buildFromBottom(
      depth: Int,
      remaining: Seq[EntryWithDepth],
      nodesByParent: Map[Option[UUID], Seq[CommentReply]],
      processedNodesById: Map[UUID, CommentNode]
  ): Seq[CommentNode] = {
    val (nodesOnCurrentDepth, rest) = remaining.span(_.depth == depth)
    val newProcessedNodes = nodesOnCurrentDepth.map { n =>
      val nodeId = n.comment.id
      val children = nodesByParent
        .getOrElse(Some(nodeId), Seq.empty)
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
      newProcessedNodes.values.toSeq
    }
  }

  @tailrec
  private def buildFromBottomTribunal(
      depth: Int,
      remaining: Seq[TribunalEntryWithDepth],
      nodesByParent: Map[Option[UUID], Seq[TribunalCommentsReply]],
      processedNodesById: Map[UUID, TribunalCommentNode]
  ): Seq[TribunalCommentNode] = {
    val (nodesOnCurrentDepth, rest) = remaining.span(_.depth == depth)
    val newProcessedNodes = nodesOnCurrentDepth.map { n =>
      val nodeId = n.comment.id
      val children = nodesByParent
        .getOrElse(Some(nodeId), Seq.empty)
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
      newProcessedNodes.values.toSeq
    }
  }

}
