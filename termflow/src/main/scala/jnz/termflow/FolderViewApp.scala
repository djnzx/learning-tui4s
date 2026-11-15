package jnz.termflow

import java.nio.file.Paths
import jnz.Action
import jnz.FolderModel
import jnz.State
import termflow.tui.*
import termflow.tui.KeyDecoder.InputKey
import termflow.tui.Tui.*
import termflow.tui.TuiPrelude.*

/** The folder browser as a TermFlow app: the model is the shown folder and the size of the terminal. */
object FolderViewApp extends TuiApp[FolderViewApp.Model, FolderViewApp.Msg]:

  final case class Model(state: State, width: Int, height: Int, input: Sub[Msg], resize: Sub[Msg])

  enum Msg:
    case Key(key: InputKey)
    case Resized(width: Int, height: Int)
    case InputError(error: Throwable)

  def toAction(key: InputKey): Option[Action] =
    key match
      case InputKey.ArrowUp | InputKey.CharKey('k')                     => Some(Action.LineUp)
      case InputKey.ArrowDown | InputKey.CharKey('j')                   => Some(Action.LineDown)
      case InputKey.PageUp                                              => Some(Action.PageUp)
      case InputKey.PageDown                                            => Some(Action.PageDown)
      case InputKey.Home                                                => Some(Action.Home)
      case InputKey.End                                                 => Some(Action.End)
      case InputKey.Enter | InputKey.ArrowRight                         => Some(Action.Open)
      case InputKey.Backspace | InputKey.ArrowLeft                      => Some(Action.Parent)
      case InputKey.F10 | InputKey.CharKey('q') | InputKey.Ctrl('C')    => Some(Action.Quit)
      case _                                                            => None

  def init(ctx: RuntimeCtx[Msg]): Tui[Model, Msg] =
    val (width, height) = (ctx.terminal.width, ctx.terminal.height)
    val start           = Paths.get("").toAbsolutePath.normalize
    Model(
      FolderModel.initial(start, FolderModel.rows(height)),
      width,
      height,
      Sub.InputKey[Msg](Msg.Key.apply, Msg.InputError.apply, ctx),
      Sub.TerminalResize(250, (w, h) => Msg.Resized(w, h), ctx)
    ).tui

  def update(model: Model, msg: Msg, ctx: RuntimeCtx[Msg]): Tui[Model, Msg] =
    msg match
      case Msg.Resized(w, h)  => model.copy(width = w, height = h).tui
      case Msg.InputError(_)  => model.tui
      case Msg.Key(key)       =>
        toAction(key) match
          case Some(Action.Quit) => Tui(model, Cmd.Exit)
          case Some(action)      => model.copy(state = FolderModel(model.state, action, FolderModel.rows(model.height))).tui
          case None              => model.tui

  def view(model: Model): RootNode = FolderView.view(model.state, model.width, model.height)

  def toMsg(input: PromptLine): Result[Msg] = Left(TermFlowError.Unexpected("no prompt in this app"))

  def main(args: Array[String]): Unit =
    val _ = args
    TuiRuntime.run(this)
